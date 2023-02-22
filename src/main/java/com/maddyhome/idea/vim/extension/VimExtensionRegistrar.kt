/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.extension

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointListener
import com.intellij.openapi.extensions.PluginDescriptor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimExtensionRegistrator
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.setToggleOption
import com.maddyhome.idea.vim.key.MappingOwner.Plugin.Companion.remove
import com.maddyhome.idea.vim.options.OptionChangeListener
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.statistic.PluginState
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

object VimExtensionRegistrar : VimExtensionRegistrator {
  internal val registeredExtensions: MutableSet<String> = HashSet()
  internal val extensionAliases = HashMap<String, String>()
  private var extensionRegistered = false
  private val logger = logger<VimExtensionRegistrar>()

  private val delayedExtensionEnabling = mutableListOf<ExtensionBeanClass>()

  @JvmStatic
  fun registerExtensions() {
    if (extensionRegistered) return
    extensionRegistered = true

    VimExtension.EP_NAME.extensions.forEach(this::registerExtension)

    VimExtension.EP_NAME.point.addExtensionPointListener(
      object : ExtensionPointListener<ExtensionBeanClass> {
        override fun extensionAdded(extension: ExtensionBeanClass, pluginDescriptor: PluginDescriptor) {
          registerExtension(extension)
        }

        override fun extensionRemoved(extension: ExtensionBeanClass, pluginDescriptor: PluginDescriptor) {
          unregisterExtension(extension)
        }
      },
      false, VimPlugin.getInstance()
    )
  }

  @Synchronized
  private fun registerExtension(extensionBean: ExtensionBeanClass) {
    val name = extensionBean.name ?: extensionBean.instance.name
    if (name in registeredExtensions) return

    registeredExtensions.add(name)
    registerAliases(extensionBean)
    VimPlugin.getOptionGroup().addOption(ToggleOption(name, getAbbrev(name), false))
    VimPlugin.getOptionGroup().addListener(name,
      object : OptionChangeListener<VimDataType> {
        override fun processGlobalValueChange(oldValue: VimDataType?) {
          if (injector.globalOptions().isSet(name)) {
            initExtension(extensionBean, name)
            PluginState.enabledExtensions.add(name)
          } else {
            extensionBean.instance.dispose()
          }
        }
      }
    )
  }

  private fun getAbbrev(name: String): String {
    return if (name == "NERDTree") "nerdtree" else name
  }

  private fun initExtension(extensionBean: ExtensionBeanClass, name: String) {
    if (injector.vimscriptExecutor.executingVimscript) {
      delayedExtensionEnabling += extensionBean
    } else {
      extensionBean.instance.init()
      logger.info("IdeaVim extension '$name' initialized")
    }
  }

  @JvmStatic
  fun enableDelayedExtensions() {
    delayedExtensionEnabling.forEach {
      it.instance.init()
      logger.info("IdeaVim extension '${it.name}' initialized")
    }
    delayedExtensionEnabling.clear()
  }

  @Synchronized
  private fun unregisterExtension(extension: ExtensionBeanClass) {
    val name = extension.name ?: extension.instance.name
    if (name !in registeredExtensions) return
    registeredExtensions.remove(name)
    removeAliases(extension)
    extension.instance.dispose()
    VimPlugin.getOptionGroup().removeOption(name)
    remove(name)
    logger.info("IdeaVim extension '$name' disposed")
  }

  override fun setOptionByPluginAlias(alias: String): Boolean {
    val name = extensionAliases[alias] ?: return false
    (injector.optionGroup.getOption(name) as? ToggleOption)?.let {
      injector.optionGroup.setToggleOption(it, OptionScope.GLOBAL)
    } ?: return false
    return true
  }

  override fun getExtensionNameByAlias(alias: String): String? {
    return extensionAliases[alias]
  }

  private fun registerAliases(extension: ExtensionBeanClass) {
    extension.aliases
      ?.mapNotNull { it.name }
      ?.forEach { alias -> extensionAliases[alias] = extension.name ?: extension.instance.name }
  }

  private fun removeAliases(extension: ExtensionBeanClass) {
    extension.aliases?.mapNotNull { it.name }?.forEach { extensionAliases.remove(it) }
  }
}
