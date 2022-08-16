# Contribution

**First of all, Thanks for contributing to this project!!!!**

## Before you contribute

**Please read [Code of Conduct](CODE_OF_CONDUCT.md) first.**

## Commits

1. English Only
2. Start with `feat: ` , `fix: ` or somethings like that
3. Each commit do ONE thing
4. Every commit must have a valid GPG signature attached

## Development

1. Make sure your editor supports EditorConfig,otherwise you should pay attention to your encoding,
   row bit sequence and other things.

2. Encode in UTF-8, with LF as the end of line sequence.

3. Config Key should be defined
   in [Defines.java](../TMessagesProj/src/main/java/top/qwq2333/nullgram/utils/Defines.java).

4. Variable naming adopts camelcase.

5. DO NOT USE FileLog. Use [Log](../TMessagesProj/src/main/java/top/qwq2333/nullgram/utils/Log.kt)
   instead.

6. Indent with 4 spaces.

7. Deprecated code should be deleted but not be commented.

8. Reformat code before you commit.

9. Except in some cases, the code header must be added

10. **At no point should you arbitrarily change [build.gradle.kts](../build.gradle.kts), especially
    upgrading `com.android.tools.build:gradle` version**
