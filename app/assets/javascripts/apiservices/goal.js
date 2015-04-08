angular.module("wust").factory("Goal", function(restmod) {
    return restmod.model("/goals").mix({
        goals: { hasMany: restmod.model("/goals") },
        problems: { hasMany: restmod.model("/problems") },
        ideas: { hasMany: restmod.model("/ideas") },
    });
});
