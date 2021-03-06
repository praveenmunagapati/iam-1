package it.infn.mw.iam.authn.x509;

import java.io.Serializable;
import java.util.Optional;

public class X509CertificateVerificationResult implements Serializable{

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public enum Status {
    SUCCESS,
    FAILED
  }
  
  final Status verificationStatus;
  final transient Optional<String> verificationError;
  
  private X509CertificateVerificationResult(Status s, String verificationError) {
    this.verificationStatus = s;
    this.verificationError = Optional.ofNullable(verificationError);
  }

  public Status status() {
    return verificationStatus;
  }

  public Optional<String> error() {
    return verificationError;
  }
  
  public static X509CertificateVerificationResult success(){
    return new X509CertificateVerificationResult(Status.SUCCESS, null);
  }
  
  public static X509CertificateVerificationResult failed(String reason){
    return new X509CertificateVerificationResult(Status.FAILED, reason);
  }

  public boolean failedVerification(){
    return verificationStatus == Status.FAILED;
  }
  
  @Override
  public String toString() {
    return "X509CertificateVerificationResult [verificationStatus=" + verificationStatus
        + ", verificationError=" + verificationError + "]";
  }
}
