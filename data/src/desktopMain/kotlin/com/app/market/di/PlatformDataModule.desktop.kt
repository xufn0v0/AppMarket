package com.app.market.di

import com.app.market.data.download.PlatformDownloadDataSource
import com.app.market.data.local.PreferencesDataSource
import com.app.market.data.local.PreferencesDataSourceImpl
import com.app.market.data.platform.DesktopThemePlatformPreferences
import com.app.market.data.platform.ThemePlatformPreferences
import com.app.market.data.platform.createHttpClient
import com.app.market.data.remote.xiaomi.platform.DesktopDeviceDefaultsDataSource
import com.app.market.data.remote.xiaomi.platform.DesktopXiaomiDeviceIdentityDataSource
import com.app.market.data.remote.xiaomi.platform.DeviceDefaultsDataSource
import com.app.market.data.remote.xiaomi.platform.XiaomiDeviceIdentityDataSource
import com.app.market.data.repository.DesktopDownloadDataSource
import com.app.market.data.repository.DesktopInstalledApkHashRepositoryImpl
import com.app.market.data.repository.DesktopInstalledPackagesRepositoryImpl
import com.app.market.data.repository.DesktopInstallerDiscoveryRepositoryImpl
import com.app.market.data.repository.DesktopInstallerPreferencesRepositoryImpl
import com.app.market.data.repository.DesktopPackageDownloader
import com.app.market.data.repository.DesktopPackageRepositoryImpl
import com.app.market.data.repository.DesktopSavedPackageRepositoryImpl
import com.app.market.domain.repository.InstalledApkHashRepository
import com.app.market.domain.repository.InstalledPackagesRepository
import com.app.market.domain.repository.InstallerDiscoveryRepository
import com.app.market.domain.repository.InstallerPreferencesRepository
import com.app.market.domain.repository.PackageRepository
import com.app.market.domain.repository.SavedPackageRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal actual val platformDataModule: Module = module {
    single { createHttpClient() }
    singleOf(::PreferencesDataSourceImpl) { bind<PreferencesDataSource>() }
    singleOf(::DesktopThemePlatformPreferences) { bind<ThemePlatformPreferences>() }
    singleOf(::DesktopDeviceDefaultsDataSource) { bind<DeviceDefaultsDataSource>() }
    singleOf(::DesktopXiaomiDeviceIdentityDataSource) { bind<XiaomiDeviceIdentityDataSource>() }
    singleOf(::DesktopInstalledPackagesRepositoryImpl) { bind<InstalledPackagesRepository>() }
    singleOf(::DesktopInstalledApkHashRepositoryImpl) { bind<InstalledApkHashRepository>() }
    singleOf(::DesktopPackageRepositoryImpl) { bind<PackageRepository>() }
    singleOf(::DesktopSavedPackageRepositoryImpl) { bind<SavedPackageRepository>() }
    singleOf(::DesktopInstallerPreferencesRepositoryImpl) { bind<InstallerPreferencesRepository>() }
    singleOf(::DesktopInstallerDiscoveryRepositoryImpl) { bind<InstallerDiscoveryRepository>() }
    single { DesktopPackageDownloader(get()) }
    singleOf(::DesktopDownloadDataSource) { bind<PlatformDownloadDataSource>() }
}
