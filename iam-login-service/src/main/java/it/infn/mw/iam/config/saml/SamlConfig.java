package it.infn.mw.iam.config.saml;

import static com.google.common.collect.Sets.newHashSet;
import static it.infn.mw.iam.authn.saml.util.Saml2Attribute.EPPN;
import static it.infn.mw.iam.authn.saml.util.Saml2Attribute.EPTID;
import static it.infn.mw.iam.authn.saml.util.Saml2Attribute.EPUID;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.velocity.app.VelocityEngine;
import org.opensaml.saml2.core.NameIDType;
import org.opensaml.saml2.metadata.provider.FileBackedHTTPMetadataProvider;
import org.opensaml.saml2.metadata.provider.FilesystemMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataFilter;
import org.opensaml.saml2.metadata.provider.MetadataFilterChain;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.saml2.metadata.provider.ResourceBackedMetadataProvider;
import org.opensaml.util.resource.ClasspathResource;
import org.opensaml.util.resource.ResourceException;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.ParserPool;
import org.opensaml.xml.parse.StaticBasicParserPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.saml.SAMLAuthenticationProvider;
import org.springframework.security.saml.SAMLBootstrap;
import org.springframework.security.saml.SAMLEntryPoint;
import org.springframework.security.saml.SAMLLogoutFilter;
import org.springframework.security.saml.SAMLLogoutProcessingFilter;
import org.springframework.security.saml.SAMLProcessingFilter;
import org.springframework.security.saml.SAMLWebSSOHoKProcessingFilter;
import org.springframework.security.saml.context.SAMLContextProvider;
import org.springframework.security.saml.context.SAMLContextProviderImpl;
import org.springframework.security.saml.context.SAMLContextProviderLB;
import org.springframework.security.saml.key.JKSKeyManager;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.log.SAMLDefaultLogger;
import org.springframework.security.saml.metadata.CachingMetadataManager;
import org.springframework.security.saml.metadata.ExtendedMetadata;
import org.springframework.security.saml.metadata.MetadataDisplayFilter;
import org.springframework.security.saml.metadata.MetadataGenerator;
import org.springframework.security.saml.metadata.MetadataGeneratorFilter;
import org.springframework.security.saml.parser.ParserPoolHolder;
import org.springframework.security.saml.processor.HTTPArtifactBinding;
import org.springframework.security.saml.processor.HTTPPAOS11Binding;
import org.springframework.security.saml.processor.HTTPPostBinding;
import org.springframework.security.saml.processor.HTTPRedirectDeflateBinding;
import org.springframework.security.saml.processor.HTTPSOAP11Binding;
import org.springframework.security.saml.processor.SAMLBinding;
import org.springframework.security.saml.processor.SAMLProcessor;
import org.springframework.security.saml.processor.SAMLProcessorImpl;
import org.springframework.security.saml.trust.httpclient.TLSProtocolConfigurer;
import org.springframework.security.saml.trust.httpclient.TLSProtocolSocketFactory;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;
import org.springframework.security.saml.util.VelocityFactory;
import org.springframework.security.saml.websso.ArtifactResolutionProfile;
import org.springframework.security.saml.websso.ArtifactResolutionProfileImpl;
import org.springframework.security.saml.websso.SingleLogoutProfile;
import org.springframework.security.saml.websso.SingleLogoutProfileImpl;
import org.springframework.security.saml.websso.WebSSOProfile;
import org.springframework.security.saml.websso.WebSSOProfileConsumer;
import org.springframework.security.saml.websso.WebSSOProfileConsumerHoKImpl;
import org.springframework.security.saml.websso.WebSSOProfileConsumerImpl;
import org.springframework.security.saml.websso.WebSSOProfileImpl;
import org.springframework.security.saml.websso.WebSSOProfileOptions;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.google.common.base.Strings;

import it.infn.mw.iam.authn.ExternalAuthenticationFailureHandler;
import it.infn.mw.iam.authn.ExternalAuthenticationSuccessHandler;
import it.infn.mw.iam.authn.InactiveAccountAuthenticationHander;
import it.infn.mw.iam.authn.RootIsDashboardSuccessHandler;
import it.infn.mw.iam.authn.TimestamperSuccessHandler;
import it.infn.mw.iam.authn.saml.CleanInactiveProvisionedAccounts;
import it.infn.mw.iam.authn.saml.DefaultSAMLUserDetailsService;
import it.infn.mw.iam.authn.saml.IamCachingMetadataManader;
import it.infn.mw.iam.authn.saml.IamExtendedMetadataDelegate;
import it.infn.mw.iam.authn.saml.IamSamlAuthenticationProvider;
import it.infn.mw.iam.authn.saml.JustInTimeProvisioningSAMLUserDetailsService;
import it.infn.mw.iam.authn.saml.MetadataLookupService;
import it.infn.mw.iam.authn.saml.SamlExceptionMessageHelper;
import it.infn.mw.iam.authn.saml.util.FirstApplicableChainedSamlIdResolver;
import it.infn.mw.iam.authn.saml.util.SamlIdResolvers;
import it.infn.mw.iam.authn.saml.util.SamlUserIdentifierResolver;
import it.infn.mw.iam.authn.saml.util.metadata.ResearchAndScholarshipMetadataFilter;
import it.infn.mw.iam.authn.saml.util.metadata.SirtfiAttributeMetadataFilter;
import it.infn.mw.iam.config.saml.SamlConfig.IamProperties;
import it.infn.mw.iam.config.saml.SamlConfig.ServerProperties;
import it.infn.mw.iam.core.time.SystemTimeProvider;
import it.infn.mw.iam.core.user.IamAccountService;
import it.infn.mw.iam.persistence.repository.IamAccountRepository;

@Configuration
@Order(value = Ordered.LOWEST_PRECEDENCE)
@Profile("saml")
@EnableConfigurationProperties({IamSamlProperties.class,
    IamSamlJITAccountProvisioningProperties.class, IamProperties.class, ServerProperties.class})
@EnableScheduling
public class SamlConfig extends WebSecurityConfigurerAdapter implements SchedulingConfigurer {

  public static final Logger LOG = LoggerFactory.getLogger(SamlConfig.class);

  @Autowired
  ResourceLoader resourceLoader;

  @Autowired
  SamlUserIdentifierResolver resolver;

  @Autowired
  IamAccountRepository repo;

  @Autowired
  IamAccountService accountService;

  @Autowired
  IamProperties iamProperties;

  @Autowired
  IamSamlProperties samlProperties;

  @Autowired
  IamSamlJITAccountProvisioningProperties jitProperties;

  @Autowired
  ServerProperties serverProperties;

  @Autowired
  InactiveAccountAuthenticationHander inactiveAccountHandler;

  @Autowired
  MetadataLookupService metadataLookupService;


  Timer metadataFetchTimer = new Timer();

  BasicParserPool basicParserPool = new BasicParserPool();

  @ConfigurationProperties(prefix = "iam")
  public static class IamProperties {

    private String baseUrl;

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }
  }

  @ConfigurationProperties(prefix = "server")
  public static class ServerProperties {

    private boolean useForwardHeaders;

    public boolean isUseForwardHeaders() {
      return useForwardHeaders;
    }

    public void setUseForwardHeaders(boolean useForwardHeaders) {
      this.useForwardHeaders = useForwardHeaders;
    }
  }

  @Configuration
  @EnableConfigurationProperties({IamSamlProperties.class})
  public static class IamSamlConfig {


    protected static final String[] DEFAULT_ID_RESOLVERS =
        {EPUID.getAlias(), EPTID.getAlias(), EPPN.getAlias()};

    @Autowired
    IamSamlProperties samlProperties;

    private String[] resolverNames() {

      if (Strings.isNullOrEmpty(samlProperties.getIdResolvers())) {
        return DEFAULT_ID_RESOLVERS;
      }

      return samlProperties.getIdResolvers().split(",");
    }

    @Bean
    public SamlUserIdentifierResolver resolver() {

      List<SamlUserIdentifierResolver> resolvers = new ArrayList<>();

      SamlIdResolvers resolverFactory = new SamlIdResolvers();

      String[] resolverNames = resolverNames();

      for (String n : resolverNames) {
        SamlUserIdentifierResolver r = resolverFactory.byName(n);
        if (r != null) {
          resolvers.add(r);
        } else {
          LOG.warn("Unsupported saml id resolver: {}", n);
        }
      }

      if (resolvers.isEmpty()) {
        throw new IllegalStateException("Could not configure SAML id resolvers");
      }

      return new FirstApplicableChainedSamlIdResolver(resolvers);
    }

  }

  @Bean
  public SAMLUserDetailsService samlUserDetailsService(SamlUserIdentifierResolver resolver,
      IamAccountRepository accountRepo, InactiveAccountAuthenticationHander handler) {

    if (jitProperties.getEnabled()) {

      return new JustInTimeProvisioningSAMLUserDetailsService(resolver, accountService, handler,
          accountRepo, jitProperties.getTrustedIdpsAsOptionalSet());
    }

    return new DefaultSAMLUserDetailsService(resolver, accountRepo, handler);

  }

  @Bean
  public VelocityEngine velocityEngine() {

    return VelocityFactory.getEngine();
  }

  // XML parser pool needed for OpenSAML parsing
  @Bean(initMethod = "initialize")
  public StaticBasicParserPool parserPool() {

    return new StaticBasicParserPool();
  }

  @Bean(name = "parserPoolHolder")
  public ParserPoolHolder parserPoolHolder() {

    return new ParserPoolHolder();
  }

  // Bindings, encoders and decoders used for creating and parsing messages
  @Bean
  public MultiThreadedHttpConnectionManager multiThreadedHttpConnectionManager() {

    return new MultiThreadedHttpConnectionManager();
  }

  @Bean
  public HttpClient httpClient() {

    return new HttpClient(multiThreadedHttpConnectionManager());
  }

  @Bean
  public SAMLAuthenticationProvider samlAuthenticationProvider(SamlUserIdentifierResolver resolver,
      IamAccountRepository accountRepo, InactiveAccountAuthenticationHander handler) {

    IamSamlAuthenticationProvider samlAuthenticationProvider =
        new IamSamlAuthenticationProvider(resolver);

    samlAuthenticationProvider
      .setUserDetails(samlUserDetailsService(resolver, accountRepo, handler));
    samlAuthenticationProvider.setForcePrincipalAsString(false);
    return samlAuthenticationProvider;
  }


  @Bean
  public SAMLContextProvider contextProvider() {

    if (serverProperties.isUseForwardHeaders()) {
      SAMLContextProviderLB cp = new SAMLContextProviderLB();

      // Assume https when sitting behind a reverse proxy
      cp.setScheme("https");

      // FIXME: find more reliable way of extracting the host name
      cp.setServerName(iamProperties.getBaseUrl().substring(8));
      cp.setServerPort(443);
      cp.setIncludeServerPortInRequestURL(false);
      cp.setContextPath("/");

      return cp;
    }

    return new SAMLContextProviderImpl();
  }

  // Initialization of OpenSAML library
  @Bean
  public static SAMLBootstrap sAMLBootstrap() {

    return new SAMLBootstrap();
  }

  // Logger for SAML messages and events
  @Bean
  public SAMLDefaultLogger samlLogger() {

    return new SAMLDefaultLogger();
  }

  private WebSSOProfileConsumerImpl setAssertionTimeChecks(WebSSOProfileConsumerImpl impl) {
    impl.setMaxAssertionTime(samlProperties.getMaxAssertionTimeSec());
    impl.setMaxAuthenticationAge(samlProperties.getMaxAuthenticationAgeSec());
    return impl;
  }

  // SAML 2.0 WebSSO Assertion Consumer
  @Bean
  public WebSSOProfileConsumer webSSOprofileConsumer() {

    return setAssertionTimeChecks(new WebSSOProfileConsumerImpl());
  }

  // SAML 2.0 Holder-of-Key WebSSO Assertion Consumer
  @Bean
  public WebSSOProfileConsumer hokWebSSOprofileConsumer() {

    return setAssertionTimeChecks(new WebSSOProfileConsumerHoKImpl());
  }

  // SAML 2.0 Web SSO profile
  @Bean
  public WebSSOProfile webSSOprofile() {

    return new WebSSOProfileImpl();
  }

  @Bean
  public SingleLogoutProfile logoutprofile() {

    return new SingleLogoutProfileImpl();
  }

  @Bean
  public KeyManager keyManager() {

    Map<String, String> passwords = new HashMap<>();
    passwords.put(samlProperties.getKeyId(), samlProperties.getKeyPassword());

    DefaultResourceLoader loader = new DefaultResourceLoader();
    Resource storeFile = loader.getResource(samlProperties.getKeystore());

    return new JKSKeyManager(storeFile, samlProperties.getKeystorePassword(), passwords,
        samlProperties.getKeyId());
  }

  //
  // Setup TLS Socket Factory
  @Bean
  public TLSProtocolConfigurer tlsProtocolConfigurer() {

    return new TLSProtocolConfigurer();
  }

  @Bean
  public ProtocolSocketFactory socketFactory() {

    return new TLSProtocolSocketFactory(keyManager(), null, "default");
  }

  @Bean
  public Protocol socketFactoryProtocol() {

    return new Protocol("https", socketFactory(), 443);
  }


  @Bean
  public WebSSOProfileOptions defaultWebSSOProfileOptions() {

    WebSSOProfileOptions webSSOProfileOptions = new WebSSOProfileOptions();
    webSSOProfileOptions.setIncludeScoping(false);

    webSSOProfileOptions.setNameID(NameIDType.PERSISTENT);

    return webSSOProfileOptions;
  }

  // Entry point to initialize authentication, default values taken from
  // properties file
  @Bean
  public SAMLEntryPoint samlEntryPoint() {

    SAMLEntryPoint samlEntryPoint = new SAMLEntryPoint();
    samlEntryPoint.setDefaultProfileOptions(defaultWebSSOProfileOptions());
    return samlEntryPoint;
  }

  @Bean
  public ExtendedMetadata extendedMetadata() {

    final String discoveryUrl = String.format("%s/saml/discovery", iamProperties.getBaseUrl());

    ExtendedMetadata extendedMetadata = new ExtendedMetadata();
    extendedMetadata.setIdpDiscoveryEnabled(true);
    extendedMetadata.setIdpDiscoveryURL(discoveryUrl);
    extendedMetadata.setSignMetadata(false);
    
    return extendedMetadata;
  }

  private void configureMetadataFiltering(MetadataProvider p, IamSamlIdpMetadataProperties props) {

    List<MetadataFilter> filters = new ArrayList<>();

    if (props.getRequireRs()) {
      filters.add(new ResearchAndScholarshipMetadataFilter());
    }

    if (props.getRequireSirtfi()) {
      filters.add(new SirtfiAttributeMetadataFilter());
    }

    if (!filters.isEmpty()) {
      try {
        
        MetadataFilterChain chain = new MetadataFilterChain();
        chain.setFilters(filters);

        p.setMetadataFilter(chain);
      } catch (MetadataProviderException e) {
        LOG.error(e.getMessage(), e);
      }
    }
  }

  private IamExtendedMetadataDelegate metadataDelegate(MetadataProvider p,
      IamSamlIdpMetadataProperties props) {

    configureMetadataFiltering(p, props);

    IamExtendedMetadataDelegate extendedMetadataDelegate =
        new IamExtendedMetadataDelegate(p, extendedMetadata());
    
    extendedMetadataDelegate.setMetadataTrustCheck(true);
    extendedMetadataDelegate.setRequireValidMetadata(true);
    
    if (props.getKeyAlias()!= null){
      extendedMetadataDelegate.setMetadataTrustedKeys(newHashSet(props.getKeyAlias()));
    }
    
    extendedMetadataDelegate.setMetadataRequireSignature(props.getRequireValidSignature());

    return extendedMetadataDelegate;
  }

  private List<MetadataProvider> metadataProviders()
      throws MetadataProviderException, IOException, ResourceException {

    List<MetadataProvider> providers = new ArrayList<>();

    for (IamSamlIdpMetadataProperties p : samlProperties.getIdpMetadata()) {
      String trimmedMedataUrl = p.getMetadataUrl().trim();

      if (trimmedMedataUrl.startsWith("classpath:")) {
        LOG.info("Adding classpath based metadata provider for URL: {}", trimmedMedataUrl);

        ClasspathResource cpMetadataResources =
            new ClasspathResource(trimmedMedataUrl.replaceFirst("classpath:", ""));

        ResourceBackedMetadataProvider metadataProvider =
            new ResourceBackedMetadataProvider(metadataFetchTimer, cpMetadataResources);

        metadataProvider.setParserPool(basicParserPool);
        providers.add(metadataDelegate(metadataProvider, p));

      } else if (trimmedMedataUrl.startsWith("file:")) {

        LOG.info("Adding File based metadata provider for URL: {}", trimmedMedataUrl);
        Resource metadataResource = resourceLoader.getResource(trimmedMedataUrl);


        FilesystemMetadataProvider metadataProvider =
            new FilesystemMetadataProvider(metadataResource.getFile());

        metadataProvider.setParserPool(basicParserPool);
        providers.add(metadataDelegate(metadataProvider, p));

      } else if (trimmedMedataUrl.startsWith("http")) {

        LOG.info("Adding HTTP metadata provider for URL: {}", trimmedMedataUrl);

        File metadataBackupFile = Files.createTempFile("metadata", "xml").toFile();
        metadataBackupFile.deleteOnExit();

        FileBackedHTTPMetadataProvider metadataProvider =
            new FileBackedHTTPMetadataProvider(metadataFetchTimer, httpClient(), trimmedMedataUrl,
                metadataBackupFile.getAbsolutePath());

        metadataProvider.setParserPool(basicParserPool);
        providers.add(metadataDelegate(metadataProvider, p));
      } else {
        LOG.error("Skipping invalid saml.idp-metatadata value: {}", trimmedMedataUrl);
      }
    }

    if (providers.isEmpty()) {
      String message = "Empty SAML metadata providers after initialization";
      LOG.error(message);
      throw new IllegalStateException(message);
    }

    return providers;
  }


  @Bean
  @Qualifier("metadata")
  public CachingMetadataManager metadata()
      throws MetadataProviderException, IOException, ResourceException {

    CachingMetadataManager manager = new IamCachingMetadataManader(metadataProviders());
    manager.setKeyManager(keyManager());
    manager.refreshMetadata();
    return manager;
  }

  @Bean
  public MetadataGenerator metadataGenerator() {

    MetadataGenerator metadataGenerator = new MetadataGenerator();

    metadataGenerator.setEntityId(samlProperties.getEntityId());
    metadataGenerator.setExtendedMetadata(extendedMetadata());
    metadataGenerator.setIncludeDiscoveryExtension(false);
    metadataGenerator.setKeyManager(keyManager());

    metadataGenerator.setEntityBaseURL(iamProperties.getBaseUrl());

    return metadataGenerator;
  }

  @Bean
  public MetadataDisplayFilter metadataDisplayFilter() {

    return new MetadataDisplayFilter();
  }


  @Bean
  public AuthenticationSuccessHandler samlAuthenticationSuccessHandler() {

    RootIsDashboardSuccessHandler sa = new RootIsDashboardSuccessHandler(iamProperties.getBaseUrl(),
        new HttpSessionRequestCache());

    return new ExternalAuthenticationSuccessHandler(new TimestamperSuccessHandler(sa, repo), "/");
  }


  @Bean
  public AuthenticationFailureHandler authenticationFailureHandler() {
    return new ExternalAuthenticationFailureHandler(new SamlExceptionMessageHelper());
  }

  @Bean
  public SAMLWebSSOHoKProcessingFilter samlWebSSOHoKProcessingFilter() throws Exception {

    SAMLWebSSOHoKProcessingFilter samlWebSSOHoKProcessingFilter =
        new SAMLWebSSOHoKProcessingFilter();
    samlWebSSOHoKProcessingFilter
      .setAuthenticationSuccessHandler(samlAuthenticationSuccessHandler());
    samlWebSSOHoKProcessingFilter.setAuthenticationManager(authenticationManager());
    samlWebSSOHoKProcessingFilter.setAuthenticationFailureHandler(authenticationFailureHandler());
    return samlWebSSOHoKProcessingFilter;
  }


  @Bean
  public SAMLProcessingFilter samlWebSSOProcessingFilter() throws Exception {

    SAMLProcessingFilter samlWebSSOProcessingFilter = new SAMLProcessingFilter();
    samlWebSSOProcessingFilter.setAuthenticationManager(authenticationManager());
    samlWebSSOProcessingFilter.setAuthenticationSuccessHandler(samlAuthenticationSuccessHandler());
    samlWebSSOProcessingFilter.setAuthenticationFailureHandler(authenticationFailureHandler());
    return samlWebSSOProcessingFilter;
  }

  @Bean
  public MetadataGeneratorFilter metadataGeneratorFilter() {

    return new MetadataGeneratorFilter(metadataGenerator());
  }

  @Bean
  public SimpleUrlLogoutSuccessHandler successLogoutHandler() {

    SimpleUrlLogoutSuccessHandler successLogoutHandler = new SimpleUrlLogoutSuccessHandler();
    successLogoutHandler.setDefaultTargetUrl("/");
    return successLogoutHandler;
  }


  @Bean
  public SecurityContextLogoutHandler logoutHandler() {

    SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
    logoutHandler.setInvalidateHttpSession(true);
    logoutHandler.setClearAuthentication(true);
    return logoutHandler;
  }

  @Bean
  public SAMLLogoutProcessingFilter samlLogoutProcessingFilter() {

    return new SAMLLogoutProcessingFilter(successLogoutHandler(), logoutHandler());
  }

  @Bean
  public SAMLLogoutFilter samlLogoutFilter() {

    return new SAMLLogoutFilter(successLogoutHandler(), new LogoutHandler[] {logoutHandler()},
        new LogoutHandler[] {logoutHandler()});
  }

  private ArtifactResolutionProfile artifactResolutionProfile() {

    final ArtifactResolutionProfileImpl artifactResolutionProfile =
        new ArtifactResolutionProfileImpl(httpClient());
    artifactResolutionProfile.setProcessor(new SAMLProcessorImpl(soapBinding()));
    return artifactResolutionProfile;
  }

  @Bean
  public HTTPArtifactBinding artifactBinding(ParserPool parserPool, VelocityEngine velocityEngine) {

    return new HTTPArtifactBinding(parserPool, velocityEngine, artifactResolutionProfile());
  }

  @Bean
  public HTTPSOAP11Binding soapBinding() {

    return new HTTPSOAP11Binding(parserPool());
  }

  @Bean
  public HTTPPostBinding httpPostBinding() {

    return new HTTPPostBinding(parserPool(), velocityEngine());
  }

  @Bean
  public HTTPRedirectDeflateBinding httpRedirectDeflateBinding() {

    return new HTTPRedirectDeflateBinding(parserPool());
  }

  @Bean
  public HTTPSOAP11Binding httpSOAP11Binding() {

    return new HTTPSOAP11Binding(parserPool());
  }

  @Bean
  public HTTPPAOS11Binding httpPAOS11Binding() {

    return new HTTPPAOS11Binding(parserPool());
  }

  @Bean
  public SAMLProcessor processor() {

    Collection<SAMLBinding> bindings = new ArrayList<>();
    bindings.add(httpRedirectDeflateBinding());
    bindings.add(httpPostBinding());
    bindings.add(artifactBinding(parserPool(), velocityEngine()));
    bindings.add(httpSOAP11Binding());
    bindings.add(httpPAOS11Binding());
    return new SAMLProcessorImpl(bindings);
  }


  /**
   * Define the security filter chain in order to support SSO Auth by using SAML 2.0
   * 
   * @return Filter chain proxy @throws Exception
   */
  @Bean
  public FilterChainProxy samlFilter() throws Exception {

    List<SecurityFilterChain> chains = new ArrayList<>();
    chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/login/**"),
        samlEntryPoint()));
    chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/logout/**"),
        samlLogoutFilter()));
    chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/metadata/**"),
        metadataDisplayFilter()));
    chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/SSO/**"),
        samlWebSSOProcessingFilter()));
    chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/SSOHoK/**"),
        samlWebSSOHoKProcessingFilter()));
    chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/SingleLogout/**"),
        samlLogoutProcessingFilter()));

    return new FilterChainProxy(chains);
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    String pattern = "/saml/**";

    http.antMatcher(pattern);

    http.csrf().ignoringAntMatchers(pattern);

    http.authorizeRequests().antMatchers(pattern).permitAll();

    http.addFilterBefore(metadataGeneratorFilter(), ChannelProcessingFilter.class)
      .addFilterAfter(samlFilter(), BasicAuthenticationFilter.class);
  }

  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {

    auth.authenticationProvider(samlAuthenticationProvider(resolver, repo, inactiveAccountHandler));
  }

  private void scheduleMetadataLookupServiceRefresh(ScheduledTaskRegistrar taskRegistrar) {
    LOG.info("Scheduling metadata lookup service refresh task to run every {} seconds.",
        samlProperties.getMetadataLookupServiceRefreshPeriodSec());
    taskRegistrar.addFixedRateTask(() -> metadataLookupService.refreshMetadata(),
        TimeUnit.SECONDS.toMillis(samlProperties.getMetadataLookupServiceRefreshPeriodSec()));
  }

  private void scheduleProvisionedAccountsCleanup(final ScheduledTaskRegistrar taskRegistrar) {

    if (!jitProperties.getEnabled()) {
      LOG.info("Just-in-time account provisioning for SAML is DISABLED.");
      return;
    }

    if (!jitProperties.getCleanupTaskEnabled()) {
      LOG.info("Cleanup for SAML JIT account provisioning is DISABLED.");
      return;
    }

    LOG.info(
        "Scheduling Just-in-time provisioned account cleanup task to run every {} seconds. Accounts inactive for {} "
            + "days will be deleted",
        jitProperties.getCleanupTaskPeriodSec(), jitProperties.getInactiveAccountLifetimeDays());

    taskRegistrar.addFixedRateTask(
        new CleanInactiveProvisionedAccounts(new SystemTimeProvider(), accountService,
            jitProperties.getInactiveAccountLifetimeDays()),
        TimeUnit.SECONDS.toMillis(jitProperties.getCleanupTaskPeriodSec()));

  }

  @Override
  public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
    scheduleProvisionedAccountsCleanup(taskRegistrar);
    scheduleMetadataLookupServiceRefresh(taskRegistrar);
  }

}
