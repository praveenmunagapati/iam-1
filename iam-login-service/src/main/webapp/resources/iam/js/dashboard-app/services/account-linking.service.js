(function() {
  'use strict';
  angular.module('dashboardApp')
      .factory('AccountLinkingService', AccountLinkingService);

  AccountLinkingService.$inject = ['$http'];

  function AccountLinkingService($http) {
    var OIDC_RESOURCE = '/iam/account-linking/OIDC';
    var SAML_RESOURCE = '/iam/account-linking/SAML';
    var X509_RESOURCE = '/iam/account-linking/X509';

    var service = {
      unlinkOidcAccount: unlinkOidcAccount,
      unlinkSamlAccount: unlinkSamlAccount,
      unlinkX509Certificate: unlinkX509Certificate
    };

    return service;

    function unlinkOidcAccount(account) {
      return $http.delete(
          OIDC_RESOURCE, {params: account});
    }

    function unlinkSamlAccount(account) {
      return $http.delete(
          SAML_RESOURCE, {params: account});
    }

    function unlinkX509Certificate(cert) {
      return $http.delete(
        X509_RESOURCE, {params: {certificateSubject: cert.subjectDn}});
    }
  }

})();