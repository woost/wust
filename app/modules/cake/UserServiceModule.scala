package modules.cake

import services.UserServiceInMemory

trait UserServiceModule {

  lazy val userService = new UserServiceInMemory

}