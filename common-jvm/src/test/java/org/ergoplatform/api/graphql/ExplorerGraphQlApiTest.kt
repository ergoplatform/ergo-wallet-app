package org.ergoplatform.api.graphql

import org.junit.Assert

internal class ExplorerGraphQlApiTest {

    //@Test
    fun getUnconfirmedBoxesById() {
        val boxInfo = ExplorerGraphQlApi("https://gql.ergoplatform.com/").getUnconfirmedBoxById("b8536e1ec7d380e02f53c34b5c5d6fe7bb24e66fd86c299efc3adbd4999155f2")

        Assert.assertFalse(boxInfo?.boxId.isNullOrEmpty())
    }
}