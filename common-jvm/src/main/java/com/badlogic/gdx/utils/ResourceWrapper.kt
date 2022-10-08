package com.badlogic.gdx.utils

import java.io.InputStream

class ResourceWrapper(private val fullResourceName: String) : I18NBundle.FileHandle {
    private val delimiter = '/'

    override fun getName(): String {
        return fullResourceName.substringAfterLast(delimiter)
    }

    override fun getPath(): String =
        fullResourceName

    override fun exists(): Boolean =
        this.javaClass.getResource(fullResourceName) != null

    override fun getStream(): InputStream =
        this.javaClass.getResource(fullResourceName).openStream()

    override fun getSibling(siblingName: String): I18NBundle.FileHandle =
        ResourceWrapper(fullResourceName.substringBeforeLast(delimiter) + delimiter + siblingName)
}