angular.module("wust").factory("Problem", function(restmod) {
    return restmod.model("/problems").mix({
        goals: { hasMany: restmod.model() },
        problems: { hasMany: restmod.model() },
        ideas: { hasMany: restmod.model() },
    });
});
