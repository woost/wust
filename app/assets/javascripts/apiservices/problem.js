angular.module("wust").factory("Problem", function(restmod) {
    return restmod.model("/problems").mix({
        goals: { hasMany: restmod.model("/goals") },
        problems: { hasMany: restmod.model("/problems") },
        ideas: { hasMany: restmod.model("/ideas") },
    });
});
