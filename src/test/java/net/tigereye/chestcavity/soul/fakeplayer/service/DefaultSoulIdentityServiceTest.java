package net.tigereye.chestcavity.soul.fakeplayer.service;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultSoulIdentityServiceTest {

  private DefaultSoulIdentityService service;

  @BeforeEach
  void setUp() {
    service = new DefaultSoulIdentityService();
  }

  @Test
  void ensureIdentityCopiesSourceProfileAndTrimsName() {
    UUID soulId = UUID.randomUUID();
    String longName = "VeryLongIdentityName";
    GameProfile source = new GameProfile(UUID.randomUUID(), longName);
    source.getProperties().put("textures", new Property("textures", "value", "signature"));

    GameProfile identity = service.ensureIdentity(soulId, source, false);

    assertNotNull(identity);
    assertSame(identity, service.getIdentity(soulId));
    assertEquals(
        source.getId(), identity.getId(), "identity should reuse source profile id when allowed");
    assertEquals(
        longName.substring(0, 16), identity.getName(), "name should be trimmed to 16 chars");
    assertEquals(1, identity.getProperties().get("textures").size(), "properties should be copied");
    Property firstProperty = identity.getProperties().get("textures").iterator().next();
    assertEquals("value", firstProperty.value());

    // ensure calling again reuses existing identity and updates properties
    GameProfile sourceUpdate = new GameProfile(UUID.randomUUID(), "ShortName");
    sourceUpdate.getProperties().put("textures", new Property("textures", "newValue"));
    GameProfile identityUpdated = service.ensureIdentity(soulId, sourceUpdate, false);
    assertNotSame(identity, identityUpdated);
    assertSame(identityUpdated, service.getIdentity(soulId));
    assertEquals(identity.getId(), identityUpdated.getId(), "identity id should remain stable");
    assertEquals("ShortName", identityUpdated.getName());
    Property updatedProperty = identityUpdated.getProperties().get("textures").iterator().next();
    assertEquals("newValue", updatedProperty.value());
  }

  @Test
  void ensureIdentityDerivesIdWhenRequested() {
    UUID soulId = UUID.randomUUID();
    GameProfile source = new GameProfile(UUID.randomUUID(), "SourceName");
    GameProfile identity = service.ensureIdentity(soulId, source, true);

    assertNotNull(identity);
    assertNotEquals(soulId, identity.getId(), "derived identity id should differ from soul id");
  }

  @Test
  void seedAndUpdateIdentityNameMaintainCache() {
    UUID soulId = UUID.randomUUID();
    service.seedIdentityName(soulId, "InitialName");
    GameProfile seeded = service.getIdentity(soulId);
    assertNotNull(seeded);
    assertEquals("InitialName", seeded.getName());

    String updatedName = "UpdatedNameExceedingLength";
    service.updateIdentityName(soulId, updatedName);
    GameProfile updated = service.getIdentity(soulId);
    assertNotNull(updated);
    assertEquals(updatedName.substring(0, 16), updated.getName());
  }

  @Test
  void isIdentityNameInUseChecksCaseInsensitive() {
    UUID soulId = UUID.randomUUID();
    service.seedIdentityName(soulId, "TestName");

    assertTrue(service.isIdentityNameInUse("testname"));
    assertTrue(service.isIdentityNameInUse("TESTNAME"));
    assertFalse(service.isIdentityNameInUse("another"));
  }

  @Test
  void cloneProfileProducesIndependentCopy() {
    GameProfile original = new GameProfile(UUID.randomUUID(), "Original");
    original.getProperties().put("textures", new Property("textures", "value"));

    GameProfile clone = service.cloneProfile(original);

    assertNotSame(original, clone);
    assertEquals(original.getId(), clone.getId());
    assertEquals(original.getName(), clone.getName());
    assertEquals(original.getProperties().size(), clone.getProperties().size());

    // mutate clone and verify original unaffected
    clone.getProperties().put("textures", new Property("textures", "newValue"));
    assertEquals(1, original.getProperties().get("textures").size());
    Property originalProperty = original.getProperties().get("textures").iterator().next();
    assertEquals("value", originalProperty.value());
  }

  @Test
  void copyPropertiesReplacesDestinationEntries() {
    GameProfile from = new GameProfile(UUID.randomUUID(), "From");
    from.getProperties().put("textures", new Property("textures", "value"));

    GameProfile to = new GameProfile(UUID.randomUUID(), "To");
    to.getProperties().put("other", new Property("other", "old"));

    service.copyProperties(from, to);

    assertTrue(to.getProperties().get("other").isEmpty(), "previous properties should be cleared");
    assertEquals(1, to.getProperties().get("textures").size());
    Property copiedProperty = to.getProperties().get("textures").iterator().next();
    assertEquals("value", copiedProperty.value());
  }

  @Test
  void putIdentityStoresAndRemovesEntries() {
    UUID soulId = UUID.randomUUID();
    GameProfile profile = new GameProfile(UUID.randomUUID(), "Profile");

    service.putIdentity(soulId, profile);
    assertTrue(service.hasIdentity(soulId));
    assertNotNull(service.getIdentity(soulId));

    service.putIdentity(soulId, null);
    assertFalse(service.hasIdentity(soulId));
    assertNull(service.getIdentity(soulId));
  }
}
