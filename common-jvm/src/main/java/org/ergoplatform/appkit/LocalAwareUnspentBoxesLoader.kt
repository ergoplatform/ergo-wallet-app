package org.ergoplatform.appkit

import org.ergoplatform.deserializeErgobox
import org.ergoplatform.persistance.PendingTransaction
import org.ergoplatform.transactions.PendingTxHelper

/**
 * WAL-aware unspent boxes loader with recording capability.
 *
 * Replaces BOTH RecordingBoxesLoader AND ExplorerAndPoolUnspentBoxesLoader.
 * Inherits:
 *   - Explorer API pagination (ExplorerApiUnspentLoader)
 *   - Mempool blacklisting (ExplorerAndPoolUnspentBoxesLoader)
 *   - Chained TX support (allowChainedTx)
 * Adds:
 *   - WAL blacklisting (inputs of pending TXs)
 *   - Change box injection (after Explorer drains)
 *   - Recording (allBoxesLoaded for consolidation)
 *   - Consolidation guard (injectedBoxIds)
 */
class LocalAwareUnspentBoxesLoader(
    private val pendingTxs: List<PendingTransaction>
) : ExplorerAndPoolUnspentBoxesLoader() {

    // ── Recording (replaces RecordingBoxesLoader) ──
    val allBoxesLoaded = ArrayList<InputBox>()

    // ── WAL blacklist ──
    private val walBlacklist: Set<String> = pendingTxs
        .flatMap { ptx ->
            ptx.inputBoxIds.split(",").filter { it.isNotBlank() }
        }.toHashSet()

    // ── Change boxes for injection ──
    private val pendingChangeBoxes: List<Pair<String, ByteArray>> = pendingTxs
        .flatMap { ptx ->
            val changeIds = ptx.changeBoxIds.split(",")
                .filter { it.isNotBlank() }
                .toSet()
            PendingTxHelper.deserializeOutputBoxes(ptx.outputBoxBytes)
                .filter { (boxId, _) -> boxId in changeIds }
        }

    // ── Consolidation guard ──
    private val _injectedBoxIds = mutableSetOf<String>()
    val injectedBoxIds: Set<String> get() = _injectedBoxIds

    // ── Per-address state ──
    private var localBoxesInjectedForAddress = false

    override fun prepareForAddress(address: Address) {
        super.prepareForAddress(address)
        localBoxesInjectedForAddress = false
    }

    override fun canUseBox(box: InputBox): Boolean {
        if (!super.canUseBox(box)) return false
        return box.id.toString() !in walBlacklist
    }

    override fun loadBoxesPage(
        ctx: BlockchainContext,
        sender: Address,
        page: Int
    ): MutableList<InputBox> {
        val parentResult = super.loadBoxesPage(ctx, sender, page)

        if (parentResult.isNotEmpty()) {
            // Record ALL boxes that pass through (for consolidation)
            allBoxesLoaded.addAll(parentResult)
            return parentResult
        }

        // Parent drained. Inject WAL change boxes (once per address)
        if (!localBoxesInjectedForAddress) {
            localBoxesInjectedForAddress = true

            val senderBase58 = sender.toString()
            val injected = mutableListOf<InputBox>()

            // Deduplication guard: if the Explorer/Mempool parent already returned
            // a change box (e.g., TX-A is already in the node mempool), our local
            // WAL copy must yield. Without this, DefaultBoxSelector would select
            // BOTH copies → duplicate input → node rejects with "Bad Request".
            val alreadyLoadedIds = allBoxesLoaded.map { it.id.toString() }.toSet()

            for ((boxId, boxBytes) in pendingChangeBoxes) {
                if (boxId in walBlacklist) continue
                if (boxId in alreadyLoadedIds) continue // Ghost Clone prevention
                try {
                    val inputBox = deserializeErgobox(boxBytes) ?: continue
                    val boxAddr = Address.fromErgoTree(
                        inputBox.ergoTree, ctx.networkType
                    ).toString()
                    if (boxAddr == senderBase58) {
                        injected.add(inputBox)
                        _injectedBoxIds.add(boxId)
                    }
                } catch (_: Throwable) {
                    // Skip malformed boxes silently
                }
            }

            if (injected.isNotEmpty()) {
                // Record injected boxes too
                allBoxesLoaded.addAll(injected)
                return injected.toMutableList()
            }
        }

        return mutableListOf()
    }
}
