package ch.cyberduck.core.gstorage;

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.Acl;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DefaultHostKeyController;
import ch.cyberduck.core.DisabledLoginController;
import ch.cyberduck.core.DisabledPasswordStore;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.Scheme;
import ch.cyberduck.core.features.Touch;
import ch.cyberduck.core.s3.S3DefaultDeleteFeature;

import org.junit.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * @version $Id$
 */
public class GoogleStorageAccessControlListFeatureTest extends AbstractTestCase {

    @Test
    public void testWrite() throws Exception {
        final Host host = new Host(new GoogleStorageProtocol(), new GoogleStorageProtocol().getDefaultHostname(), new Credentials(
                properties.getProperty("google.projectid"), null
        ));
        final GoogleStorageSession session = new GoogleStorageSession(host);
        session.open(new DefaultHostKeyController());
        session.login(new DisabledPasswordStore() {
            @Override
            public String getPassword(final Scheme scheme, final int port, final String hostname, final String user) {
                if(user.equals("Google OAuth2 Access Token")) {
                    return properties.getProperty("google.accesstoken");
                }
                if(user.equals("Google OAuth2 Refresh Token")) {
                    return properties.getProperty("google.refreshtoken");
                }
                return null;
            }
        }, new DisabledLoginController());
        final Path container = new Path("test.cyberduck.ch", Path.DIRECTORY_TYPE);
        final Path test = new Path(container, UUID.randomUUID().toString(), Path.FILE_TYPE);
        session.getFeature(Touch.class).touch(test);
        final GoogleStorageAccessControlListFeature f = new GoogleStorageAccessControlListFeature(session);
        final Acl acl = new Acl();
        acl.addAll(new Acl.GroupUser(Acl.GroupUser.EVERYONE), new Acl.Role(Acl.Role.READ));
        acl.addAll(new Acl.GroupUser(Acl.GroupUser.AUTHENTICATED), new Acl.Role(Acl.Role.READ));
        f.setPermission(test, acl);
        acl.addAll(new Acl.CanonicalUser("00b4903a976d2139c3b4ffbe7c61eccdb69e545fde42445d455befdad73b1455", "dkocher"), new Acl.Role(Acl.Role.FULL));
        assertEquals(acl, f.getPermission(test));
        new S3DefaultDeleteFeature(session).delete(Collections.<Path>singletonList(test), new DisabledLoginController());
        session.close();
    }
}
