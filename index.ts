import { Platform } from 'react-native';
import { requireOptionalNativeModule } from 'expo-modules-core';

interface AppShortcutNative {
  isSupported(): boolean;
  pin(options: {
    id: string;
    label: string;
    uri: string;
    iconUrl?: string | null;
  }): Promise<boolean>;
}

const native = requireOptionalNativeModule<AppShortcutNative>('AppShortcut');

export interface PinShortcutInput {
  /** Stable identifier for the shortcut. Re-used IDs replace the existing pin. */
  id: string;
  /** Label shown under the icon on the launcher. */
  label: string;
  /** Deep link URI opened when the shortcut is tapped (e.g. `myapp://path`). */
  uri: string;
  /** Optional remote favicon URL. Falls back to the consumer app's launcher icon. */
  iconUrl?: string | null;
}

export const appShortcut = {
  isSupported(): boolean {
    if (Platform.OS !== 'android' || !native) return false;
    try {
      return native.isSupported();
    } catch {
      return false;
    }
  },

  async pin(input: PinShortcutInput): Promise<boolean> {
    if (Platform.OS !== 'android' || !native) return false;
    return native.pin(input);
  },
};
