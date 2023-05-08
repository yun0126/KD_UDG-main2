package com.joljak.myapplication
import java.io.FileOutputStream
import java.io.Writer
import java.io.OutputStream
import java.io.OutputStreamWriter


//    class XmlWriter(outputStream: OutputStream, encoding: String?) {
class XmlWriter(writer: OutputStreamWriter) {
    private val out: OutputStreamWriter = writer

    private var depth = 0
    private var startTagInProgress = false

    private val namespaces = HashMap<String, String>()

    fun startDocument(encoding: String) {
        out.write("<?xml version=\"1.0\" encoding=\"")
        out.write(encoding)
        out.write("\"?>")
    }

    fun setPrefix(prefix: String, namespace: String) {
        namespaces[prefix] = namespace
    }

    fun startTag(namespace: String?, name: String) {
        if (startTagInProgress) {
            closeStartTag()
        }
        doIndent()
        newline()
        out.write("<")
        if (!namespace.isNullOrEmpty()) {
            out.write(namespace)
            out.write(":")
        }
        out.write(name)
        startTagInProgress = true
        depth++
    }

    fun attribute(namespace: String?, name: String, value: String) {
        out.write("\n")
        out.write(" ")
        if (!namespace.isNullOrEmpty()) {
            out.write(namespace)
            out.write(":")
        }
        out.write(name)
        out.write("=\"")
//        out.write(escapeXml(value))
        out.write(value)
        out.write("\"")
    }

    fun text(text: String) {
        if (startTagInProgress) {
            closeStartTag()
        }
//        out.write(escapeXml(text))
        out.write(text)
    }


    fun endTag(namespace: String?, name: String) {
        depth--
        if (startTagInProgress) {
            out.write("/>")
            startTagInProgress = false
        } else {
            doIndent()
            out.write("</")
            if (!namespace.isNullOrEmpty()) {
                out.write(namespace)
                out.write(":")
            }
            out.write(name)
            out.write(">")
        }
    }

    fun endDocument() {
        out.flush()
    }

    private fun closeStartTag() {
        out.write(">")
        startTagInProgress = false
    }

    fun newline() {
        out.write("\n")
    }

    private fun doIndent() {
        for (i in 0 until depth) {
            out.write("    ")
        }
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    companion object {
        fun create(outputStream: OutputStream, encoding: String): XmlWriter {
            val writer = OutputStreamWriter(outputStream, encoding)
            return XmlWriter(writer)
        }

    }
    fun close() {
        out.close()
    }
}
