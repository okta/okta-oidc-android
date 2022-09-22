# Releasing
1. Update version number in [gradle.properties](gradle.properties), [CHANGELOG.md](CHANGELOG.md), and [README.md](README.md) and merge the PR
2. Run publish task in [bacon](https://bacon-go.aue1e.saasure.net/tasks/RUN_GRADLE_PUBLISH_TASK)
1. Selecting artifact `okta-oidc-android`
2. Setting ROBO_ACTUAL_COMMAND to `gradleOpenPublish`
3. `git checkout master && git pull`
4. `git tag -s -a 1.0.0 -m "Release 1.0.0"` - Update the version number to the one you're trying to publish
5. `git push --tags`
6. Update [github release page](https://github.com/okta/okta-oidc-android/releases) with the latest release notes

## Local Publishing
If the bacon task above isn't available, you can publish locally via:
1. `./gradlew publish --rerun-tasks -PsignWithGpgCommand`
2. `./gradlew closeAndReleaseRepository`

## Generating GPG Keys
- TLDR: `gpg --gen-key`
- [Maven Central GPG Docs](https://central.sonatype.org/publish/requirements/gpg/)

## Other means of signing
- See [Publishing Docs](https://github.com/vanniktech/gradle-maven-publish-plugin#signing)
- If using an in memory GPG Key:
    - Export the secret key with something along the lines of `gpg --output ~/Desktop/OktaPrivate.pgp --armor --export-secret-key jay.newstrom@okta.com`
    - Then you can format it correctly using something along the lines of `cat ~/Desktop/OktaPrivate.pgp | grep -v '\-\-' | grep -v '^=.' | tr -d '\n'`
