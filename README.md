# 🛡️ SecCCE: Secure Cloud Correlation Engine

**A Secure, Cloud-based SIEM Correlation Engine using a stateless FaaS model.**

> **Context:** SecCCE is a security-hardened extension of the [CCE (Cloud Correlation Engine)](https://github.com/aserckumecka/CCE) project. While CCE focuses on performance, automation, and cost-optimization for translating SIEM rules into serverless functions, SecCCE focuses on **confidentiality and secure execution** in untrusted public cloud environments.

SecCCE builds upon the CCE foundation by introducing critical cryptographic layers. It ensures that security events, alerts, and correlation states remain encrypted both in transit and at rest, utilizing managed key infrastructures to prevent unauthorized data exposure during the FaaS execution lifecycle.

## Key Security Enhancements
* 🔒 **End-to-End Envelope Encryption:** Security events and correlation state variables are encrypted before leaving the edge/on-premise environment and are only decrypted in-memory during Lambda execution.
* 🔑 **AWS KMS Integration:** Leverages the AWS Key Management Service (KMS) for secure, centralized handling of Master Keys, preventing hardcoded secrets and unauthorized decryption.
* 🛡️ **Blind Cloud Processing:** DynamoDB tables and S3 buckets store only encrypted payloads. The cloud provider cannot natively inspect the raw security logs or SIEM alerts.
* ✅ **Full CCE Compatibility:** Retains all core CCE capabilities, including automated rule translation and stateless correlation optimization.

## 🚀 Getting Started

### Prerequisites
In addition to the standard CCE requirements (AWS CLI, Maven, Java 11, S3 Bucket, DynamoDB access), SecCCE strictly requires:
* **AWS KMS Key:** You must create a Symmetric Encryption Key in AWS KMS and obtain its Key ID.
* **IAM Permissions:** The IAM User and the `LambdaFullAccess` Role must include KMS permissions (e.g., `kms:Encrypt`, `kms:Decrypt`, `kms:GenerateDataKey`) for the specific Key ID used.

### Configuration
1. **Configure AWS Credentials:** Run `aws configure` on your local machine.
2. **Edit Configuration:** Open `SecCCE-engine/cce.conf` and update the infrastructure details:
   * `S3-bucket name` (e.g., cce-bucket)
   * `AWS region` (e.g., sa-east-1)
   * `AWS account ID`
   * `Master Key ID` (e.g., e14654f1-d482-4aa9-b14e-42157be667ff)

> ⚠️ **CRITICAL: Git Security & Repository Management**
> Never commit the `cce.conf` file containing your real AWS Account ID or KMS Master Key ID to GitHub. Your repository must contain a robust `.gitignore` file that ignores the real configuration file. We recommend versioning only a template file (e.g., `cce.conf.example`).
> If you accidentally tracked the file previously, remove it from the Git index without deleting your local copy by running:
> `git rm --cached SecCCE-engine/cce.conf`

## ▶️ Running SecCCE
**1. Compile the Code:**
Navigate to the `SecCCE-engine/` directory and build the project:
```bash
mvn clean compile assembly:single
