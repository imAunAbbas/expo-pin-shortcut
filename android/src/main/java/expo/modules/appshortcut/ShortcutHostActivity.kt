package expo.modules.appshortcut

import android.app.Activity
import android.os.Bundle

// Stable anchor for pinned shortcuts. Android removes pinned shortcuts
// whose owning activity is disabled — and the default owner is the
// main launcher activity, which apps that swap launcher icons (via
// activity-aliases) routinely disable. Anchoring shortcuts to this
// always-enabled, never-launched activity keeps them alive across
// icon-swap cycles. The shortcut's actual click intent is unchanged
// (still ACTION_VIEW on the user-supplied uri); this activity only
// exists as metadata for ShortcutInfo.setActivity().
class ShortcutHostActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    finish()
  }
}
