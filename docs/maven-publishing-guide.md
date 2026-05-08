# HuiiAd SDK 发布指南

## Maven 发布选项

### 选项 1: JitPack（推荐，最简单）

JitPack 可以直接从 GitHub 仓库发布，无需额外配置。

**使用方法：**

1. 在 GitHub 上创建 Release
2. 在 JitPack.io 网站输入你的仓库地址
3. 获取依赖

**Gradle 配置：**

```gradle
// settings.gradle
dependencyResolutionManagement {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}

// app/build.gradle
dependencies {
    implementation 'com.github.huiiad:f5en1unqbj:ssp-sdk:1.0.0'
}
```

---

### 选项 2: GitHub Packages（免费，适合私有/公开仓库）

**步骤 1: 创建 GitHub Personal Access Token**

1. 进入 GitHub → Settings → Developer settings → Personal access tokens
2. 点击 "Generate new token (classic)"
3. 勾选权限：`write:packages`
4. 保存 token

**步骤 2: 配置 Gradle**

在 `~/.gradle/gradle.properties` 中添加：

```properties
gpr.user=你的GitHub用户名
gpr.token=你的GitHub Personal Access Token
```

**步骤 3: 发布**

```bash
./gradlew ssp_sdk:publishReleasePublicationToGitHubPackagesRepository
```

**使用方法：**

```gradle
// settings.gradle
dependencyResolutionManagement {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/huiiad/f5en1unqbj")
            credentials {
                username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.token") ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

// app/build.gradle
dependencies {
    implementation 'io.github.huiiad:ssp-sdk:1.0.0'
}
```

---

### 选项 3: Maven Central（最正式，需要 Sonatype 账号）

**步骤 1: 注册 Sonatype Jira 账号**

1. 访问 https://issues.sonatype.org
2. 创建 Jira 账号
3. 创建 Issue 申请 Group ID（例如 `io.github.huiiad`）

**步骤 2: 配置签名密钥**

```properties
# ~/.gradle/gradle.properties
signing.keyId=你的Key ID
signing.password=你的密钥密码
signing.secretKeyRingFile=~/.gnupg/secring.gpg
```

**步骤 3: 更新 build.gradle**

```gradle
publishing {
    publications {
        release(MavenPublication) {
            // ... 其他配置 ...

            pom {
                // ...

                repositories {
                    maven {
                        name = "MavenCentral"
                        url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                        credentials {
                            username = System.getenv("OSSRH_USERNAME")
                            password = System.getenv("OSSRH_PASSWORD")
                        }
                    }
                }
            }
        }
    }
}
```

**步骤 4: 发布**

```bash
./gradlew ssp_sdk:publishReleasePublicationToMavenCentralRepository
```

---

### 选项 4: 私有 Maven 服务器（使用阿里云/腾讯云）

**步骤 1: 创建私有仓库**

在阿里云/腾讯云容器镜像服务中创建 Maven 仓库。

**步骤 2: 配置**

```gradle
// settings.gradle
dependencyResolutionManagement {
    repositories {
        maven {
            name = "AliyunMaven"
            url = 'https://maven.aliyun.com/repository/public'
            credentials {
                username = '你的用户名'
                password = '你的密码'
            }
        }
    }
}
```

---

## 推荐的 Gradle 配置（settings.gradle）

```gradle
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    projects {
        include ':ssp_sdk'
    }
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
        // 如果使用 GitHub Packages，取消下面的注释
        // maven {
        //     name = "GitHubPackages"
        //     url = uri("https://maven.pkg.github.com/huiiad/f5en1unqbj")
        //     credentials {
        //         username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
        //         password = project.findProperty("gpr.token") ?: System.getenv("GITHUB_TOKEN")
        //     }
        // }
    }
}

rootProject.name = "HuiiAdSdkProject"
include ':ssp_sdk'
include ':demo_app'
```

## 使用示例

```gradle
// app/build.gradle
dependencies {
    implementation 'io.github.huiiad:ssp-sdk:1.0.0'
}
```

## 常见问题

### Q: 如何发布新版本？

1. 修改 `ssp_sdk/build.gradle` 中的 `POM_VERSION`
2. 创建 GitHub Release 或运行发布命令
3. 等待仓库同步（通常 10-30 分钟）

### Q: 如何让用户看到最新版本？

在文档中添加版本检查说明，或使用 `+` 代替版本号（不推荐生产环境）：

```gradle
implementation 'io.github.huiiad:ssp-sdk:+'
```

### Q: 如何处理依赖冲突？

```gradle
implementation('io.github.huiiad:ssp-sdk:1.0.0') {
    exclude group: 'com.android.support', module: 'appcompat-v7'
}
```
