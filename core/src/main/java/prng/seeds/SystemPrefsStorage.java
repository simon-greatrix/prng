package prng.seeds;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.prefs.Preferences;

import prng.SecureRandomProvider;

/**
 * Store seed data in the JVM's system preferences storage
 * 
 * @author Simon Greatrix
 *
 */
public class SystemPrefsStorage extends PreferenceStorage {

    public SystemPrefsStorage() throws StorageException {
        // no-op
    }

    @Override
    protected Preferences getPreferences() throws StorageException {
        try {
            return AccessController.doPrivileged(
                    new PrivilegedAction<Preferences>() {
                        @Override
                        public Preferences run() {
                            return Preferences.systemNodeForPackage(
                                    SeedStorage.class);
                        }
                    });
        } catch (SecurityException e) {
            SecureRandomProvider.LOG.warn(
                    "Lacking permission \"RuntimePermission preferences\" or access to system preferences - cannot access seed data in system preferences");
            throw new StorageException(
                    "Privilege 'preferences' is required to use preferences",
                    e);
        }
    }
}
