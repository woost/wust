angular.module("wust").factory("Idea", function(restmod) {
    return restmod.model("/ideas").mix({
        goals: { hasMany: restmod.model() },
        problems: { hasMany: restmod.model() },
        ideas: { hasMany: restmod.model() },
    });
});
