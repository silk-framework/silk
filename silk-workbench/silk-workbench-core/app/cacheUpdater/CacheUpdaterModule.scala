package cacheUpdater

import play.api.inject._

class CacheUpdaterModule extends SimpleModule(bind[CacheUpdaterTask].toSelf.eagerly())