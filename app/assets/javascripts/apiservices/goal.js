angular.module("wust").factory("Goal", function(restmod) {
    return restmod.model("/goals").mix({
        goals: { hasMany: restmod.model() },
        problems: { hasMany: restmod.model() },
        ideas: { hasMany: restmod.model() },
    });
});
