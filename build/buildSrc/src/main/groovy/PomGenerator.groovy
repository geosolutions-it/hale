// Fraunhofer Institute for Computer Graphics Research (IGD)
// Department Graphical Information Systems (GIS)
//
// Copyright (c) 2004-2013 Fraunhofer IGD. All rights reserved.
//
// This source code is property of the Fraunhofer IGD and underlies
// copyright restrictions. It may only be used with explicit
// permission from the respective owner.

import groovy.text.GStringTemplateEngine
import org.gradle.api.Project
import java.nio.file.Path

class PomGenerator {
    private Project project

    PomGenerator(Project project) {
        this.project = project
    }

    /**
     * Creates a generic pom file for an OSGi bundle using the specified packaging
     */
    def makePluginPomFileWithPackaging(symbolicName, version, needsScala, needsGroovy, packaging, path, isJavaProject = false) {
        makePomFileWithPackaging(symbolicName, version, needsScala, needsGroovy, packaging, 'pom-plugin.xml', path, isJavaProject)
    }
	
	/**
	 * Resolve a template file.
	 */
	def resolveTemplate(def name) {
		Helper.resolveTemplate(project, name)
	}

    /**
     * Creates a generic pom file for an OSGi bundle using the specified packaging and template file
     */
    def makePomFileWithPackaging(symbolicName, version, needsScala, needsGroovy, packaging, templateName, path, isJavaProject = false) {
        // calculate relative path to pom.xml of parent project
		Path rootPath = project.ext.rootDir.toPath()
		Path relativeRoot = path.toPath().relativize(rootPath)
		def relativePath = relativeRoot.toString()
		
        new File(path, 'pom.xml').withWriter { w ->
            def template = new GStringTemplateEngine().createTemplate(resolveTemplate(templateName))
            def result = template.make([
                'groupId': project.group,
                'artifactId': symbolicName,
                'version': version,
                'packaging': packaging,
                'parentGroupId': project.group,
                'parentArtifactId': project.ext.parentArtifactId,
                'parentVersion': project.version + project.ext.versionSuffix,
                'parentRelativePath': relativePath,
                'needsScala': needsScala,
				'needsGroovy': needsGroovy,
				'extraRequirements': project.ext.extraRequirements,
				'isJavaProject': isJavaProject
            ]).toString()
            w << result
        }
    }

    /**
     * Creates a generic pom file for an OSGi bundle
     */
    def makePomFile(symbolicName, version, needsScala, needsGroovy, path, isJavaProject = false) {
        def packaging = 'eclipse-plugin'
        if (symbolicName.endsWith('.test')) {
            packaging = 'eclipse-test-plugin'
        }
        if (project.ext.generateArtifacts) {
            packaging = 'jar'
        }
        version += project.ext.versionSuffix
        makePluginPomFileWithPackaging(symbolicName, version, needsScala, needsGroovy, packaging, path, isJavaProject)
    }

    /**
     * Generate pom.xml files for all bundles
     */
    def generatePomFiles() {
		def bundleParser = new BundleParser(project)
		
		// generate pom files for all bundles
        def parsedBundles = bundleParser.getParsedBundles()
        println('Generating pom.xml files for bundles ...')

        parsedBundles.each {
            makePomFile(it.key, it.value.version, it.value.needsScala, it.value.needsGroovy, it.value.path, it.value.isJavaProject)
        }

		// generate pom files for all features
		def parsedFeatures = bundleParser.getParsedFeatures()
		println('Generating pom.xml files for features ...')

		parsedFeatures.each {
			println it.key
			makePomFileWithPackaging(it.key, it.value.version, false, false, 'eclipse-feature', 'pom-feature.xml', it.value.path)
		}

        // generate pom file for target platform
		println('Generating pom.xml file for target platform ...')
        new File(project.ext.platformBundle, 'pom.xml').withWriter { w ->
            // calculate relative path to pom.xml of parent project
            Path rootPath = project.ext.rootDir.toPath()
            Path relativeRoot = project.ext.platformBundle.toPath().relativize(rootPath)
            def relativePath = relativeRoot.toString()

            def template = new GStringTemplateEngine().createTemplate(resolveTemplate('pom-platform.xml'))
            def result = template.make([
                    'groupId': project.group,
                    'version': project.version + project.ext.versionSuffix,
                    'parentGroupId': project.group,
                    'parentArtifactId': project.ext.parentArtifactId,
                    'parentRelativePath': relativePath,
                    'parentVersion': project.version + project.ext.versionSuffix,
					'platformClassifier': project.ext.platformFileName
            ]).toString()
            w << result
        }
    }

    /**
     * Generates a pom file for the parent project and adds the given modules. The
     * modules must be passed in the same format as the hash returned by BundleParser#getParsedBundles()
     */
    def generateParentPomFile(additionalModules = [:]) {
        new File(project.ext.rootDir, 'pom.xml').withWriter { w ->
			Path rootPath = project.ext.rootDir.toPath()
			
            def template = new GStringTemplateEngine().createTemplate(resolveTemplate('pom-parent.xml'))
			def bundleParser = new BundleParser(project)
            def bundles = bundleParser.getParsedBundles()
			def features = bundleParser.getParsedFeatures()
            def result = template.make([
                    'groupId': project.group,
                    'version': project.version + project.ext.versionSuffix,
                    'parentGroupId': project.group,
                    'parentArtifactId': project.ext.parentArtifactId,
                    'parentVersion': project.version + project.ext.versionSuffix,
                    'modules': (bundles + features + additionalModules).values().collect {
						rootPath.relativize(it.path.toPath()).toString()
                    }.sort(),
		    'platformPath': rootPath.relativize(project.ext.platformBundle.toPath()).toString(),
                    'envOs': project.ext.osgiOS,
                    'envWs': project.ext.osgiWS,
                    'envArch': project.ext.osgiArch,
                    'generateArtifacts': project.ext.generateArtifacts,
					'platformClassifier': project.ext.platformFileName
            ]).toString()
            w << result
        }
    }
}
