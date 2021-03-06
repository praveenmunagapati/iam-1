package it.infn.mw.iam.test.ext_authn.saml;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.opensaml.saml2.core.NameID;
import org.springframework.security.saml.SAMLCredential;

import it.infn.mw.iam.authn.saml.util.NameIdUserIdentifierResolver;
import it.infn.mw.iam.authn.saml.util.Saml2Attribute;
import it.infn.mw.iam.authn.saml.util.SamlIdResolvers;
import it.infn.mw.iam.authn.saml.util.SamlUserIdentifierResolver;
import it.infn.mw.iam.persistence.model.IamSamlId;

public class ResolverTests {


  @Test
  public void testSamlIdResolverAttributeResolution(){
    SamlIdResolvers resolvers = new SamlIdResolvers();
    
    for (Saml2Attribute a: Saml2Attribute.values()){
      Assert.assertNotNull(resolvers.byName(a.getAlias()));
    }
    
  }
  
  @Test
  public void emptyNameIdResolverTest() {

    SAMLCredential cred = Mockito.mock(SAMLCredential.class);
    Mockito.when(cred.getNameID()).thenReturn(null);

    SamlUserIdentifierResolver resolver = new NameIdUserIdentifierResolver();

    Optional<IamSamlId> resolvedId = resolver.resolveSamlUserIdentifier(cred).getResolvedId();

    Assert.assertFalse(resolvedId.isPresent());

  }

  @Test
  public void nameIdResolverTest() {
    NameID nameId = Mockito.mock(NameID.class);
    Mockito.when(nameId.getValue()).thenReturn("nameid");
    Mockito.when(nameId.getFormat()).thenReturn("format");

    SAMLCredential cred = Mockito.mock(SAMLCredential.class);
    Mockito.when(cred.getNameID()).thenReturn(nameId);
    Mockito.when(cred.getRemoteEntityID()).thenReturn("entityId");


    SamlUserIdentifierResolver resolver = new NameIdUserIdentifierResolver();

    IamSamlId resolvedId = resolver.resolveSamlUserIdentifier(cred).getResolvedId()
      .orElseThrow(() -> new AssertionError("Could not resolve nameid SAML ID"));

    Assert.assertThat(resolvedId.getUserId(), Matchers.equalTo("nameid"));
    Assert.assertThat(resolvedId.getIdpId(), Matchers.equalTo("entityId"));
    Assert.assertThat(resolvedId.getAttributeId(), Matchers.equalTo("format"));
  }

  @Test
  public void mailIdResolverTest() {
    SamlIdResolvers resolvers = new SamlIdResolvers();

    SamlUserIdentifierResolver resolver = resolvers.byAttribute(Saml2Attribute.MAIL);

    assertThat(resolver, is(not(nullValue())));
    
    SAMLCredential cred = Mockito.mock(SAMLCredential.class);
    Mockito.when(cred.getAttributeAsString(Saml2Attribute.MAIL.getAttributeName()))
      .thenReturn("test@test.org");
    Mockito.when(cred.getRemoteEntityID()).thenReturn("entityId");

    IamSamlId resolvedId = resolver.resolveSamlUserIdentifier(cred).getResolvedId()
      .orElseThrow(() -> new AssertionError("Could not resolve email address SAML ID"));

    assertThat(resolvedId.getUserId(), equalTo("test@test.org"));
    assertThat(resolvedId.getAttributeId(),
        equalTo(Saml2Attribute.MAIL.getAttributeName()));

    Assert.assertThat(resolvedId.getIdpId(), equalTo("entityId"));

  }
}
