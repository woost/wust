angular.module("wust.api").config(CacheModelConfig);

CacheModelConfig.$inject = ["CacheModelProvider"];

function CacheModelConfig(CacheModelProvider) {
    CacheModelProvider.setCache("CacheService");
}
