package arrow.meta.plugin.gradle

import com.sun.org.apache.xerces.internal.parsers.DOMParser
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.xml.sax.InputSource
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

open class InstallIdeaPlugin: DefaultTask() {

  // TODO: Consider release version

  @TaskAction
  fun installPlugin() {
    if (!inIdea()) {
      println("Run this task from Intellij IDEA")
      return
    }

    if (!pluginsDirExists()) {
      println("Plugins directory not found")
      return
    }

    if (ideaPluginExists()) {
      println("Arrow Meta IDEA Plugin is already installed!")
      return
    }

    println("Arrow Meta IDEA Plugin is not installed! Downloading ...")
    val properties = Properties()
    properties.load(this.javaClass.getResourceAsStream("plugin.properties"))
    val compilerPluginVersion = properties.getProperty("COMPILER_PLUGIN_VERSION")
    val parser = DOMParser()
    parser.parse(InputSource(URL("https://meta.arrow-kt.io/idea-plugin/snapshots/$compilerPluginVersion/updatePlugins.xml").openStream()))
    val artifactURL = parser.document.getElementsByTagName("plugin").item(0).attributes.getNamedItem("url").nodeValue
    Files.copy(
      URL(artifactURL).openStream(),
      Paths.get(pluginsDir(), File(artifactURL).name)
    )
    // TODO: dynamic plugin to avoid restarting
    println("Restart Intellij IDEA to finish the installation!")
  }
}

internal fun inIdea(): Boolean =
  System.getProperty("idea.active") == "true"

internal fun pluginsDirExists(): Boolean =
  when {
    System.getProperty("idea.plugins.path") != null ->
      File(System.getProperty("idea.plugins.path")).exists()
    System.getProperty("jb.vmOptionsFile") != null ->
      File(pluginsDirFromVmOptionsFile()).exists()
    else ->
      false
  }

private fun pluginsDir(): String =
  when {
    System.getProperty("idea.plugins.path") != null ->
      System.getProperty("idea.plugins.path")
    else -> {
      pluginsDirFromVmOptionsFile()
    }
  }

internal fun ideaPluginExists(): Boolean =
  File(pluginsDir()).listFiles().any { it.name.startsWith("idea-plugin") }

private fun pluginsDirFromVmOptionsFile(): String {
  val configDir = File(System.getProperty("jb.vmOptionsFile")).parent
  return Paths.get(configDir, "plugins").toString()
}