# ZIO Logging

Always follow these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.

ZIO Logging is a purely functional logging library for Scala applications using the ZIO ecosystem. It's a multi-module Scala project that supports JVM, JavaScript, and Native platforms for the core module, with additional backend integrations for SLF4J v1/v2, Java Platform Logging (JPL), and java.util.logging (JUL) bridge.

## Working Effectively

### Environment Setup
- Ensure Java 17+ is installed (the project uses Java 17 by default)
- Use the provided `./sbt` script in the repository root - do NOT install SBT separately
- The project uses SBT 1.11.6 as specified in `project/build.properties`

### Bootstrap and Build Commands
- NEVER CANCEL build commands - builds can take 30-45 minutes. ALWAYS set timeout to 60+ minutes minimum
- Initial dependency download: `./sbt update` -- takes 15-20 minutes. NEVER CANCEL.
- Compile all modules: `./sbt +Test/compile` -- takes 30-45 minutes. NEVER CANCEL. Set timeout to 60+ minutes.
- Build artifacts: `./sbt +publishLocal` -- takes 20-30 minutes. NEVER CANCEL.
- Run all tests: `./sbt +test` -- takes 20-25 minutes. NEVER CANCEL. Set timeout to 45+ minutes.

### Testing Commands
- Test specific module: `./sbt coreJVM/test` or `./sbt slf4j/test`
- Compile examples: `./sbt examplesCoreJVM/compile examplesCoreJS/compile examplesCoreNative/compile`
- Compile all examples and benchmarks: 
  ```bash
  ./sbt examplesCoreJVM/compile examplesCoreJS/compile examplesCoreNative/compile \
        examplesJpl/compile examplesJulBridge/compile examplesSlf4j2Bridge/compile \
        examplesSlf4jLogback/compile examplesSlf4j2Logback/compile \
        examplesSlf4j2Log4j/compile benchmarks/compile
  ```

### Documentation and Website
- Build documentation website: `./sbt docs/buildWebsite` -- takes 10-15 minutes
- Generate README: `./sbt docs/generateReadme`
- Clean docs: `./sbt docs/clean`

### Code Quality and Linting
- ALWAYS run linting before committing: `./sbt lint` -- takes 5-10 minutes
- Format code: `./sbt scalafmtAll`
- Check formatting: `./sbt scalafmtCheckAll`
- Check workflow is up to date: `./sbt ciCheckGithubWorkflow`

### Running Examples
Examples are located in `examples/` directory with subdirectories:
- `examples/core/` - Core logging examples (JVM, JS, Native)
- `examples/slf4j-logback/` - SLF4J v1 with Logback
- `examples/slf4j2-logback/` - SLF4J v2 with Logback
- `examples/slf4j2-log4j/` - SLF4J v2 with Log4j
- `examples/jpl/` - Java Platform Logging examples
- `examples/jul-bridge/` - java.util.logging bridge examples
- `examples/slf4j2-bridge/` - SLF4J v2 bridge examples

To run examples:
```bash
./sbt examplesCoreJVM/run
./sbt examplesSlf4jLogback/run
```

## Validation Scenarios

### After Making Changes
- ALWAYS compile the affected modules: `./sbt [module]/compile`
- ALWAYS run tests for affected modules: `./sbt [module]/test`
- ALWAYS run the linter: `./sbt lint`
- For core changes, test examples: `./sbt examplesCoreJVM/run`
- For backend changes, test corresponding examples (e.g., `./sbt examplesSlf4jLogback/run`)

### End-to-End Validation
- Compile all: `./sbt +Test/compile` (30-45 min, NEVER CANCEL)
- Test all: `./sbt +test` (20-25 min, NEVER CANCEL)  
- Build docs: `./sbt docs/buildWebsite` (10-15 min)
- Run core example to verify basic functionality works

## Critical Build Information

### Timeouts and Timing
- **NEVER CANCEL** any build command - they can take 30+ minutes
- Initial `./sbt update`: 15-20 minutes
- Full compilation `./sbt +Test/compile`: 30-45 minutes - set timeout to 60+ minutes
- Full test suite `./sbt +test`: 20-25 minutes - set timeout to 45+ minutes
- Documentation build: 10-15 minutes
- Linting: 5-10 minutes

### Known Issues
- Network connectivity issues may cause dependency resolution failures
- If you see "UnknownHostException" errors, retry the command
- The `sbt-jcstress` plugin may fail to resolve - this is a known issue and can be ignored
- Use `./sbt` script, not system SBT installation

## Project Structure

### Core Modules
- `core/` - Main ZIO Logging implementation (supports JVM, JS, Native)
- `slf4j/` - SLF4J v1.x integration 
- `slf4j2/` - SLF4J v2.x integration
- `slf4j-bridge/` - Bridge for using ZIO Logging with SLF4J v1 libraries
- `slf4j2-bridge/` - Bridge for using ZIO Logging with SLF4J v2 libraries  
- `jpl/` - Java Platform Logging integration
- `jul-bridge/` - java.util.logging bridge
- `benchmarks/` - Performance benchmarks
- `docs/` - Documentation and website generation

### Configuration Files
- `build.sbt` - Main build configuration
- `project/Versions.scala` - Dependency versions
- `project/plugins.sbt` - SBT plugins
- `.scalafmt.conf` - Code formatting rules
- `.scalafix.conf` - Code refactoring rules
- `.jvmopts` - JVM options for builds

### CI/CD Integration
- GitHub Actions workflow in `.github/workflows/ci.yml`
- Tests on Java 11, 17, and 21
- Cross-compilation for Scala 2.12, 2.13, and 3.x
- Automated README generation and documentation publishing

## Common Tasks

### Adding a New Feature
1. Modify relevant module in `core/`, `slf4j/`, etc.
2. Add tests in the same module's `src/test/scala/`
3. Update examples if applicable
4. Run: `./sbt [module]/compile [module]/test lint`
5. Test examples: `./sbt examples[Module]/run`

### Updating Dependencies
1. Modify `project/Versions.scala`
2. Run: `./sbt update` (15-20 min)
3. Run full test suite: `./sbt +test` (20-25 min, NEVER CANCEL)

### Documentation Changes
1. Edit files in `docs/` directory
2. Build: `./sbt docs/buildWebsite` (10-15 min)
3. Generate README: `./sbt docs/generateReadme`

Always ensure changes compile and pass tests before committing. The CI system will run comprehensive checks across multiple Java versions and Scala versions.