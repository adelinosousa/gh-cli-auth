GRADLE = ./gradlew
TEST_PROJECT = test-project
LOCAL_VERSION = local-SNAPSHOT

.PHONY: check publish test-project-install test-project-uninstall test-project-inspect test-project-all clean

## Run all checks (unit + functional tests + validation)
check:
	$(GRADLE) check

## Publish plugin to local Maven repo with snapshot version
publish:
	GRADLE_PUBLISH_VERSION=$(LOCAL_VERSION) $(GRADLE) publishToMavenLocal

## Install the init script via test project
test-project-install: publish
	cd $(TEST_PROJECT) && ../gradlew ghCliAuthInstall

## Remove the init script via test project
test-project-uninstall:
	cd $(TEST_PROJECT) && ../gradlew ghCliAuthUninstall

## Show the generated init script
test-project-inspect:
	@cat ~/.gradle/init.d/gh-cli-auth.init.gradle.kts

## Full cycle: publish, install, inspect
test-project-all: test-project-install test-project-inspect

## Clean all build caches
clean:
	$(GRADLE) clean
