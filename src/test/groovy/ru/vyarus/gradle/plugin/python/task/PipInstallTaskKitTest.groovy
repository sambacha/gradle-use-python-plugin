package ru.vyarus.gradle.plugin.python.task

import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.python.AbstractKitTest
import ru.vyarus.gradle.plugin.python.cmd.Pip

/**
 * @author Vyacheslav Rusakov
 * @since 20.11.2017
 */
class PipInstallTaskKitTest extends AbstractKitTest {

    @Override
    def setup() {
        // make sure correct version installed
        new Pip(ProjectBuilder.builder().build()).install('click==6.7')
    }

    def "Check no declared modules"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
            
            python.scope = USER

        """

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "no all modules list printed"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.task(':pipInstall').outcome == TaskOutcome.SKIPPED
    }

    def "Check no modules list"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                pip 'click:6.7'
                scope = USER
                showInstalledVersions = false
            }

        """

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "no all modules list printed"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        !result.output.contains('python -m pip install click')
        !result.output.contains('python -m pip list')

        when: "run one more time"
        result = run('pipInstall')

        then: "up to date"
        result.task(':pipInstall').outcome == TaskOutcome.UP_TO_DATE
    }

    def "Check always install"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                scope = USER
                pip 'click:6.7'
                alwaysInstallModules = true
            }

        """

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "click install called"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output.contains('Requirement already satisfied: click==6.7')
        result.output =~ /python(3)? -m pip list/
    }

    def "Check custom task"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
            
            python.scope = USER
            
            task customPip(type: PipInstallTask) {                
                pip 'click:6.7'
                alwaysInstallModules = true
            }

        """

        when: "run task"
        BuildResult result = run('customPip')

        then: "click install called"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.task(':customPip').outcome == TaskOutcome.SUCCESS
        result.output.contains('Requirement already satisfied: click==6.7')
        result.output =~ /python(3)? -m pip list/
    }

    def "Check extra index urls and trusted hosts options "() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
            
            python {
                scope = USER
                pip 'click:6.7'
                alwaysInstallModules = true
                extraIndexUrls "http://extra-url.com"
                trustedHosts "extra-url.com" 
            }
        """

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "arguments applied"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output =~ /python(3)? -m pip install click==6.7 --user --extra-index-url http:\/\/extra-url.com --trusted-host extra-url.com/
    }

    def "Check applying custom arguments"() {

        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
            
            python {
                scope = USER
                pip 'click:6.7'
                alwaysInstallModules = true
            }
            
            pipInstall.options('--upgrade-strategy', 'only-if-needed')
        """

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "arguments applied"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output =~ /python(3)? -m pip install click==6.7 --user --upgrade-strategy only-if-needed/
    }
}
