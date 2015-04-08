angular.module("wust").factory("Idea", function(restmod) {
    return restmod.model("/ideas").mix({
        goals: { hasMany: restmod.model("/goals") },
        problems: { hasMany: restmod.model("/problems") },
        ideas: { hasMany: restmod.model("/ideas") },
    });
});
