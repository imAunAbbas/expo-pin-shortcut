import ExpoModulesCore

// iOS does not expose a programmatic API for adding home screen icons.
// This module is intentionally a no-op on iOS so the JS surface stays
// cross-platform. See the README for rationale.
public class AppShortcutModule: Module {
  public func definition() -> ModuleDefinition {
    Name("AppShortcut")

    Function("isSupported") {
      return false
    }

    AsyncFunction("pin") { (_: [String: Any?]) -> Bool in
      throw UnsupportedException()
    }
  }
}

internal class UnsupportedException: Exception {
  override var code: String { "E_UNSUPPORTED" }
  override var reason: String {
    "iOS does not provide a programmatic API for pinning home screen shortcuts"
  }
}
