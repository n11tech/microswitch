# 🚀 Microswitch - Deployment Strategy Library

**Microswitch**, Java Spring Boot uygulamalarında **Canary**, **Shadow** ve **Blue/Green** deployment stratejilerini programmatik olarak yönetmenizi sağlayan güçlü bir kütüphanedir.

## 📋 İçindekiler

- [Özellikler](#-özellikler)
- [Kurulum](#-kurulum)
- [Hızlı Başlangıç](#-hızlı-başlangıç)
- [Deployment Stratejileri](#-deployment-stratejileri)
- [Konfigürasyon](#-konfigürasyon)
- [Metrics ve Monitoring](#-metrics-ve-monitoring)
- [API Referansı](#-api-referansı)
- [Örnekler](#-örnekler)
- [Best Practices](#-best-practices)
- [Troubleshooting](#-troubleshooting)

## ✨ Özellikler

- 🎯 **3 Deployment Stratejisi**: Canary, Shadow, Blue/Green
- ⚡ **Lazy Evaluation**: Sadece seçilen method çalıştırılır
- 📊 **Prometheus Metrics**: Otomatik performans izleme
- 🔧 **Esnek Konfigürasyon**: YAML tabanlı ayarlar
- 🏗️ **Spring Boot Auto-Configuration**: Otomatik bean yapılandırması
- 🎛️ **Actuator Endpoints**: Runtime konfigürasyon görüntüleme
- 🧪 **Production-Ready**: Test edilmiş ve optimize edilmiş

## 📦 Kurulum

### Maven

```xml
<dependency>
    <groupId>com.n11.development</groupId>
    <artifactId>microswitch</artifactId>
    <version>1.0</version>
</dependency>
```

### Gradle

```gradle
implementation 'com.n11.development:microswitch:1.0'
```

## 🚀 Hızlı Başlangıç

### 1. Application Properties

```yaml
microswitch:
  services:
    user-service:
      enabled: true
      canary:
        primaryPercentage: 80
        algorithm: "random"
      shadow:
        weight: 5
      blueGreen:
        primaryPercentage: 70
        ttl: 3600
```

### 2. Service Implementation

```java
@Service
public class UserService {
    
    @Autowired
    private DeploymentManager deploymentManager;
    
    public User createUser(String name) {
        return deploymentManager.canary(
            () -> createUserV1(name),  // Stable version
            () -> createUserV2(name),  // New version
            "user-service"
        );
    }
    
    private User createUserV1(String name) {
        // Legacy implementation
        return new User(name, "v1");
    }
    
    private User createUserV2(String name) {
        // New implementation
        return new User(name, "v2");
    }
}
```

## 🎯 Deployment Stratejileri

### Canary Deployment
Trafiğin belirli bir yüzdesini yeni versiyona yönlendirir.

```java
// %80 stable, %20 experimental
String result = deploymentManager.canary(
    this::stableMethod,
    this::experimentalMethod,
    "service-key"
);
```

### Shadow Deployment
Yeni versiyonu paralel çalıştırır, sonucu stable'dan döner.

```java
// Stable döner, experimental paralel çalışır
Integer result = deploymentManager.shadow(
    this::stableMethod,
    this::experimentalMethod,
    "service-key"
);
```

### Blue/Green Deployment
TTL tabanlı veya yüzdelik dağılımla version seçimi yapar.

```java
// Primary percentage veya TTL tabanlı seçim
String result = deploymentManager.blueGreen(
    this::blueMethod,
    this::greenMethod,
    "service-key"
);
```

## ⚙️ Konfigürasyon

### Tam Konfigürasyon Örneği

```yaml
microswitch:
  services:
    payment-service:
      enabled: true
      canary:
        primaryPercentage: 90      # %90 stable, %10 experimental
        algorithm: "random"        # "random" veya "sequence"
      shadow:
        weight: 10                 # Her 10 çağrıda 1 shadow execution
      blueGreen:
        primaryPercentage: 80      # %80 blue, %20 green
        ttl: 7200                  # 2 saat sonra tamamen green'e geç
    
    user-service:
      enabled: false               # Devre dışı - sadece stable çalışır
```

### Konfigürasyon Parametreleri

| Parametre | Açıklama | Varsayılan |
|-----------|----------|------------|
| `enabled` | Servis aktif mi? | `true` |
| `canary.primaryPercentage` | Stable method yüzdesi | `100` |
| `canary.algorithm` | Dağılım algoritması | `"sequence"` |
| `shadow.weight` | Shadow execution sıklığı | `5` |
| `blueGreen.primaryPercentage` | Blue method yüzdesi | `100` |
| `blueGreen.ttl` | Green'e geçiş süresi (saniye) | `null` |

## 📊 Metrics ve Monitoring

### Prometheus Metrics

Microswitch otomatik olarak aşağıdaki metrikleri toplar:

```
# Başarılı deployment sayıları
deployment_success_total{service="user-service",version="stable",strategy="canary"} 85
deployment_success_total{service="user-service",version="experimental",strategy="canary"} 15

# Shadow executions
deployment_success_total{service="payment-service",version="shadow_execution",strategy="shadow"} 12
```

### Actuator Endpoints

```bash
# Mevcut konfigürasyonu görüntüle
GET /actuator/microswitch

# Örnek response
{
  "services": {
    "user-service": {
      "enabled": true,
      "canary": {
        "primaryPercentage": 80,
        "algorithm": "random"
      }
    }
  }
}
```

## 📚 API Referansı

### DeploymentManager

```java
public class DeploymentManager {
    
    // Canary deployment
    public <R> R canary(Supplier<R> stable, Supplier<R> experimental, String serviceKey);
    
    // Shadow deployment  
    public <R> R shadow(Supplier<R> stable, Supplier<R> experimental, String serviceKey);
    
    // Blue/Green deployment
    public <R> R blueGreen(Supplier<R> blue, Supplier<R> green, String serviceKey);
}
```

### Supplier Kullanımı

```java
// Method reference (önerilen)
deploymentManager.canary(this::methodV1, this::methodV2, "service");

// Lambda expression
deploymentManager.canary(
    () -> processPayment(request),
    () -> processPaymentNew(request),
    "payment"
);

// Direkt method call (lazy evaluation için önerilmez)
// deploymentManager.canary(() -> method1(), () -> method2(), "service");
```

## 💡 Örnekler

### E-Commerce Ödeme Sistemi

```java
@Service
@Slf4j
public class PaymentService {
    
    @Autowired
    private DeploymentManager deploymentManager;
    
    public PaymentResult processPayment(PaymentRequest request) {
        return deploymentManager.canary(
            () -> processWithOldProvider(request),
            () -> processWithNewProvider(request),
            "payment-processing"
        );
    }
    
    private PaymentResult processWithOldProvider(PaymentRequest request) {
        log.info("Processing with legacy provider");
        // Legacy payment logic
        return PaymentResult.success("OLD_PROVIDER");
    }
    
    private PaymentResult processWithNewProvider(PaymentRequest request) {
        log.info("Processing with new provider");
        // New payment logic
        return PaymentResult.success("NEW_PROVIDER");
    }
}
```

### Kullanıcı Doğrulama

```java
@Service
public class AuthService {
    
    @Autowired
    private DeploymentManager deploymentManager;
    
    public boolean authenticateUser(String username, String password) {
        return deploymentManager.shadow(
            () -> authenticateWithLDAP(username, password),
            () -> authenticateWithOAuth(username, password),
            "user-auth"
        );
    }
    
    // Shadow: LDAP sonucu döner, OAuth paralel test edilir
    private boolean authenticateWithLDAP(String username, String password) {
        // LDAP authentication
        return ldapService.authenticate(username, password);
    }
    
    private boolean authenticateWithOAuth(String username, String password) {
        // OAuth authentication (shadow testing)
        return oauthService.authenticate(username, password);
    }
}
```

### Veri Analizi

```java
@Service
public class AnalyticsService {
    
    @Autowired
    private DeploymentManager deploymentManager;
    
    public AnalyticsReport generateReport(String userId) {
        return deploymentManager.blueGreen(
            () -> generateReportV1(userId),
            () -> generateReportV2(userId),
            "analytics-report"
        );
    }
    
    // Blue/Green: TTL sonrası tamamen V2'ye geçer
}
```

## 🏆 Best Practices

### 1. Service Key Naming
```java
// ✅ İyi
"user-registration"
"payment-processing"
"order-fulfillment"

// ❌ Kötü
"service1"
"test"
"temp"
```

### 2. Method Reference Kullanımı
```java
// ✅ İyi - Lazy evaluation
deploymentManager.canary(this::methodV1, this::methodV2, "service");

// ❌ Kötü - Eager evaluation
deploymentManager.canary(() -> methodV1(), () -> methodV2(), "service");
```

### 3. Error Handling
```java
public Result processData(String data) {
    try {
        return deploymentManager.canary(
            () -> processV1(data),
            () -> processV2(data),
            "data-processing"
        );
    } catch (Exception e) {
        log.error("Deployment failed, using fallback", e);
        return processV1(data); // Fallback to stable
    }
}
```

### 4. Monitoring
```java
// Metrics'leri kontrol edin
@EventListener
public void onApplicationReady(ApplicationReadyEvent event) {
    log.info("Microswitch metrics available at /actuator/prometheus");
    log.info("Microswitch config available at /actuator/microswitch");
}
```

## 🔧 Troubleshooting

### Yaygın Problemler

#### 1. Bean Bulunamıyor
```
Error: No qualifying bean of type 'DeploymentManager'
```
**Çözüm**: `@ComponentScan` içinde `com.n11.development` paketini ekleyin.

```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.yourpackage", "com.n11.development"})
public class Application {
    // ...
}
```

#### 2. Konfigürasyon Yüklenmiyor
**Çözüm**: `application.yml` dosyasında `microswitch` konfigürasyonunu kontrol edin.

#### 3. Metrics Görünmüyor
**Çözüm**: Micrometer ve Actuator dependencies'lerini ekleyin:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### Debug Modu

```yaml
logging:
  level:
    com.n11.development: DEBUG
```

### Health Check

```java
@Component
public class MicroswitchHealthCheck {
    
    @Autowired
    private DeploymentManager deploymentManager;
    
    @EventListener
    public void checkHealth(ApplicationReadyEvent event) {
        try {
            String result = deploymentManager.canary(
                () -> "healthy",
                () -> "healthy",
                "health-check"
            );
            log.info("Microswitch health check: {}", result);
        } catch (Exception e) {
            log.error("Microswitch health check failed", e);
        }
    }
}
```

## 📈 Performans

- **Lazy Evaluation**: Sadece seçilen method çalıştırılır
- **Minimal Overhead**: ~1ms ek latency
- **Memory Efficient**: Düşük memory footprint
- **Thread Safe**: Concurrent execution desteği

## 🤝 Katkıda Bulunma

1. Fork edin
2. Feature branch oluşturun (`git checkout -b feature/amazing-feature`)
3. Commit yapın (`git commit -m 'Add amazing feature'`)
4. Push edin (`git push origin feature/amazing-feature`)
5. Pull Request açın

## 📄 Lisans

Bu proje MIT lisansı altında lisanslanmıştır. Detaylar için [LICENSE](LICENSE) dosyasına bakın.

## 📞 Destek

- 📧 Email: development@n11.com
- 🐛 Issues: [GitHub Issues](https://github.com/n11-development/microswitch/issues)
- 📖 Docs: [Documentation](DEPLOYMENT_USAGE.md)

---

**Made with ❤️ by N11 Development Team**