# 🚀 Microswitch - Deployment Stratejileri Kütüphanesi

## 📋 Genel Bakış

**Microswitch**, **Canary**, **Shadow** ve **Blue/Green** deployment stratejilerini abstract factory pattern ile sunan temiz ve sade bir Spring Boot kütüphanesidir.

## 🏗️ Mimari Yapı

```
DeploymentManager (Facade Pattern)
    ↓
DeploymentStrategyFactory (Abstract Factory Pattern)
    ↓
DefaultDeploymentStrategyFactory (Concrete Implementation)
    ↓
Strategy Classes (Canary, Shadow, BlueGreen) + DeployTemplate (Template Method Pattern)
    ↓
DeploymentMetrics (Prometheus Integration)
```

## 🎯 Özellikler

- ✅ **Type-safe**: Generic type safety
- ✅ **Auto-configuration**: Spring Boot otomatik konfigürasyon
- ✅ **Metrics**: Prometheus metrics entegrasyonu
- ✅ **Extensible**: Yeni stratejiler kolayca eklenebilir

## 🔧 Kullanım Örnekleri

### 1. Spring Bean Olarak Enjeksiyon

```java
@Service
public class UserService {
    
    @Autowired
    private DeploymentManager deploymentManager;
    
    public String getUserInfo(String userId) {
        return deploymentManager.canary(
            () -> legacyUserService.getUser(userId),      // Stable service (lazy)
            () -> newUserService.getUser(userId),         // Experimental service (lazy)
            "user-service"                                // Service key
        );
    }
}
```

### 2. Canary Deployment (Aşamalı Geçiş)

```java
// Method reference ile (lazy evaluation)
String result = deploymentManager.canary(
    this::legacyMethod,        // Sadece seçilirse çalışır
    this::newMethod,           // Sadece seçilirse çalışır
    "payment-service"
);

// Lambda ile
String result = deploymentManager.canary(
    () -> "Legacy Service Response",
    () -> "New Service Response", 
    "payment-service"
);
```

### 3. Shadow Deployment (Paralel Test)

```java
// Method reference ile
Integer result = deploymentManager.shadow(
    this::legacyCalculation,   // Ana sonuç
    this::newCalculation,      // Paralel test
    "calculation-service"
);

// Lambda ile
Integer result = deploymentManager.shadow(
    () -> 100,    // Primary result
    () -> 95,     // Shadow result (karşılaştırma için)
    "calculation-service"
);
```

### 4. Blue/Green Deployment (Tamamen Geçiş)

```java
// Method reference ile
String result = deploymentManager.blueGreen(
    this::blueMethod,          // Blue version
    this::greenMethod,         // Green version
    "user-service"
);

// Lambda ile
String result = deploymentManager.blueGreen(
    () -> "Blue Version Response",
    () -> "Green Version Response",
    "user-service"
);
```

## ⚙️ Konfigürasyon

### Maven Dependency

```xml
<dependency>
    <groupId>com.n11.development</groupId>
    <artifactId>microswitch</artifactId>
    <version>1.0</version>
</dependency>
```

### application.yml

```yaml
microswitch:
  enabled: true  # Kütüphaneyi etkinleştir/devre dışı bırak
  services:
    payment-service:
      enabled: true
      canary:
        primary-percentage: 75        # %75 stable, %25 experimental
        algorithm: random            # random veya sequence
    calculation-service:
      enabled: true
      shadow:
        weight: 3                    # Her 3. çağrıda shadow çalıştır
    user-service:
      enabled: true
      blue-green:
        primary-percentage: 50       # %50 blue, %50 green
        ttl: 3600                   # 1 saat sonra tamamen green'e geç
```

### Actuator Endpoints

- **GET** `/actuator/microswitch` - Konfigürasyon görüntüleme (null'lar gizli)
- **GET** `/actuator/deployment-metrics` - Deployment metrikleri

## 📊 Prometheus Metrics

Kütüphane otomatik olarak aşağıdaki metrikleri üretir:

```
# Başarılı deployment'lar
deployment_success_total{service="payment-service",version="stable",strategy="canary"} 150

# Hatalı deployment'lar  
deployment_error_total{service="payment-service",version="experimental",strategy="canary"} 5

# Shadow deployment doğruluk oranı
deployment_success_total{service="calculation-service",version="shadow_accuracy",strategy="shadow"} 95
```

### Metrics Endpoint'i

```json
GET /actuator/deployment-metrics
{
  "payment-service_canary_success_rate": 96.77,
  "calculation-service_shadow_accuracy_rate": 95.0,
  "user-service_bluegreen_status": "active"
}
```

## 🧪 Test Senaryoları

### Unit Test

```java
@ExtendWith(MockitoExtension.class)
class DeploymentManagerTest {
    
    @Mock private DeploymentManager deploymentManager;
    
    @Test
    void testCanaryDeployment() {
        when(deploymentManager.canary(any(), any(), eq("test-service")))
            .thenReturn("New Version Response");
            
        String result = deploymentManager.canary(
            () -> "Old Version",
            () -> "New Version",
            "test-service"
        );
        
        assertEquals("New Version Response", result);
    }
}
```

### Integration Test

```java
@SpringBootTest
@TestPropertySource(properties = {
    "microswitch.services.test-service.enabled=true",
    "microswitch.services.test-service.canary.primary-percentage=50"
})
class MicroswitchIntegrationTest {
    
    @Autowired private DeploymentManager deploymentManager;
    
    @Test
    void testRealDeployment() {
        String result = deploymentManager.canary(
            () -> "Stable Version",
            () -> "Experimental Version",
            "test-service"
        );
        
        assertThat(result).isIn("Stable Version", "Experimental Version");
    }
}
```

## 🚀 Gelişmiş Özellikler

### Strategy Türleri

| Strateji | Açıklama | Kullanım Senaryosu |
|----------|----------|-------------------|
| **Canary** | Aşamalı geçiş (%X stable, %Y experimental) | Yeni özellik test etme |
| **Shadow** | Paralel çalıştırma (ana sonuç + gölge test) | Performans karşılaştırma |
| **Blue/Green** | Tamamen geçiş (TTL sonrası %100 yeni) | Versiyon yükseltme |

### Algoritma Türleri

- **`random`**: Rastgele dağılım
- **`sequence`**: Sıralı dağılım (1,2,3,4,5... pattern)

### Konfigürasyon İpuçları

```yaml
microswitch:
  services:
    critical-service:
      enabled: true
      canary:
        primary-percentage: 95  # Kritik servisler için düşük risk
        algorithm: sequence     # Öngörülebilir dağılım
    test-service:
      enabled: true  
      shadow:
        weight: 1              # Her çağrıda shadow (test ortamı)
```

## 📚 Best Practices

1. **Canary**: Kritik servisler için düşük yüzde (%5-10) ile başlayın
2. **Shadow**: Test ortamında weight=1, prod'da weight=10+ kullanın  
3. **Blue/Green**: TTL değerini servise göre ayarlayın (1-24 saat)
4. **Monitoring**: Metrics'leri sürekli izleyin, hata oranı artarsa geri alın

## 🔧 Troubleshooting

### Bean bulunamıyor hatası
```
@ComponentScan(basePackages = "com.n11.development")
```

### Metrics görünmüyor
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,deployment-metrics,microswitch
```
 Kütüphane Hazır!** Başka projenizde kullanmak için dependency'yi ekleyin ve konfigürasyonu yapın. 🚀