Gradle workspace Plugin
=======================

This is a plugin to manage multi-project workspaces. Or at least some day it may be ;-)

Currently it allows you to very conveniently switch between source and binary dependencies while developing. If you are working on a project, and you want to include a binary dependency in source form, just check out the dependency and add it to your build. Any dependencies to the binary will automatically be replaced by dependencies to the added project without having to change all the build files back and forth.

Applying the plugin
-------------------

### Gradle 2.1 and higher

    plugins {
        id 'me.seeber.workspace' version '0.3.0'
    }

### Gradle 1.x and 2.0

    buildscript {
        repositories {
            jcenter()
        }

        dependencies {
            classpath 'gradle.plugin.me.seeber:gradle-workspace-plugin:0.2.0'
        }
    }

    apply plugin: 'me.seeber.workspace'

Usage
-----

Just apply the plugin, that's all. It will scan all your dependencies and do its magic.

Examples
--------

The main use case is currently if you want to work on an application that depends on some libraries that are not part of the application. You might want to start working on the application, and then later pull in some dependencies temporarily as needed without having to change your build files.

Let's say you want to contribute to Gradle, so you check it out from Github and hack away:

    # git clone https://github.com/gradle/gradle.git

After some time you realize that your cool new plugin requires some additional language features in Groovy. Since Groovy probably has uses other than as part of Gradle, you probably don't want to permanently include it in Gradle. You go ahead and clone Groovy from Github:

    # git clone https://github.com/groovy/groovy-core.git

Now to work on both together, just create a new settings file named e.g. 'settings.gradle' with the following content:

    include 'gradle'
    include 'groovy-core'

    # If you need to adjust the paths to your projects, you can do this:
    project (':gradle').projectDir = new File(settingsDir, 'gradle')
    project (':groovy-core').projectDir = new File(settingsDir, 'groovy-core')

Also create a new build file named 'build.gradle' with the following content:

    plugins {
        id 'me.seeber.workspace' version '0.3.0'
    }

Now you can work seamlessly on both projects at once, and any binary dependencies will be replaced by a dependencies on the included project. No need to do a 'gradle install' to have one project pick up changes in the other project, and 'gradle eclipse' will give you nice Eclipse workspace with direct project dependencies.

Roadmap
-------

* Add tasks to manage workspace to dynamically add or remove subprojects

License
-------

This plugin is licensed under the [BSD 2-Clause](http://opensource.org/licenses/BSD-2-Clause) license.
