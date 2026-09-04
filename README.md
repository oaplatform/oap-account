# OAP Account

**OAP Account** is a modular authentication and account management component for the [Open Application Platform (OAP)](https://github.com/oaplatform/oap).  
It provides APIs and implementations for handling user accounts, social login integration, and testing utilities.

## Features

- 🔐 Account management: registration, authentication, password handling.
- 🌐 Social login support (e.g., Google, Facebook).
- 🧪 Test utilities for integration and unit testing.
- 🧩 Modular structure for easy extension and integration:
    - `oap-account-api`: Interfaces and base contracts.
    - `oap-account`: Core implementation.
    - `oap-account-social`: OAuth/social login support.
    - `oap-account-test`: Test tools and mocks.

## Getting Started

### Prerequisites

- Java 22+
- Maven 3.8+

### Installation

To include in your Maven project:

```xml
<dependency>
  <groupId>oap</groupId>
  <artifactId>oap-account</artifactId>
  <version>${oap.project.version}</version>
</dependency>
