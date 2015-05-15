Gradle workspace Plugin
=======================

This is a plugin to manage multi-project workspaces. Or at least some day it may be ;-)

It allows you to very conveniently switch between source and binary dependencies while developing. If you are working on a project, and you want to include a binary dependency in source form, just check out the dependency and add it to your build. Any dependencies to the binary will automatically be replaced by dependencies to the added project without having to change all the build files back and forth.

Applying the plugin
-------------------

### Gradle 2.1 and higher

    plugins {
        id 'me.seeber.github.gradle-workspace-plugin' version '0.1.0'
    }

### Gradle 1.x and 2.0

    buildscript {
        repositories {
            jcenter()
        }
        dependencies {
            classpath 'me.seeber.github.gradle-workspace-plugin:0.1.0'
        }
    }

    apply plugin: 'me.seeber.workspace'

Usage
-----

Just apply the plugin, that's all. It will scan all your dependencies and do its magic.

Examples
--------

Coming soon.

License
-------

This plugin is licensed under the [BSD 2-Clause](http://opensource.org/licenses/BSD-2-Clause) license.
