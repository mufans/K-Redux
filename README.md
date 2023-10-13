# K-Redux
Kotlin redux 状态管理框架

参考前端 **Redux** 实现的 **Kotlin** 状态管理框架。

**Redux** 作为 **MVI** 架构原型基础 ，提供唯一可信数据源、单向数据流的状态更新以及订阅。



### K-Redux 的几大角色：

- **Store** 状态容器，可包含多种状态，可作为全局或者局部的状态容器
- **State** 代表状态，**Store** 对外提供 **StateFlow** 作为状态的聚合用来进行订阅

- **Action** 对应 **Intent** 代表意图

- **Reducer** 响应 **Action** 并更新状态

- **Middleware** 拦截**Action**, 实现切面逻辑

- **SingleEvent** 则是提供单次事件订阅



#### Store 构建

```kotlin
Store
.StoreBuilder(initState) // 初始状态
.addActionToReducer(createActionToReducer()) // Action 和 Reducer的关系转换
.addSingleEventReducer(::reducerToEvent) // Reducer 和 单次Event的转换
.addMiddleware(middleware) // 中间件，处理切面逻辑
.build()
```



#### Action To Reducer

```kotlin
// 处理登出
private fun FlowAction.logoutAction() = filterIsInstance<LoginIntent.Logout>().map {
    LoginReducer.Logout
}

// 处理登录
private fun FlowAction.loginAction() = filterIsInstance<LoginIntent.RequestLogin>().map {
    val resp = MockUserRepository.login(it.name, it.pass)
    if (resp.success) {
        LoginReducer.Success(it.name, resp.token ?: "")
    } else {
        LoginReducer.Failure(resp.message ?: "")
    }
}

// 将Action 转换为Reducer
private fun createActionToReducer(): FlowAction.() -> Flow<Reducer<LoginState>> {
    return {
          // 合并多个Action
          merge(loginAction(), logoutAction())
    }
 }
```



#### Reducer to SingleEvent

```kotlin
 private fun reducerToEvent(reducer: Reducer<LoginState>): Event? {
        return when (reducer) {
            is LoginReducer.Success -> LoginEvent.Success
            is LoginReducer.Failure -> LoginEvent.Failure(reducer.message)
            else -> null
        }
 }
```



#### Middleware

```kotlin
class LoginMiddleware : Middleware<LoginState> {
    override suspend fun handle(store: Store<LoginState>, action: Action): Action {
        // 拦截Action 处理相关逻辑
        return action
    }
}
```



#### 状态订阅

```kotlin
store.stateFlow.collect() // 订阅状态

store.singleEvent.collect() // 订阅事件
```



#### 结合Android ViewModel使用

```kotlin
// 继承MviViewModel
class LoginViewModel : MviViewModel<LoginState, LoginIntent, LoginEvent>()

// 实现如下三个协议

// 意图
interface MviIntent : Action

// 状态
interface MviState : State

// 事件
interface MviEvent : Event


viewModel.dispatch(intent) // 发送意图

viewModel.stateFlow.collect() // 订阅状态

viewModel.singleEvent.collect() // 订阅事件
```



#### 路由

```kotlin
// 通过实现IRouterContract接口，可实现路由拦截
interface IRouterContract<T : State> {

    val actionToReducer: Flow<Action>.() -> Flow<Reducer<T>>

    val middleware: Middleware<T>?

    val singleEventReducer: SingleEventReducer<T>?
}

StoreBuilder.addRouterContract(contract) // 添加路由协议

routeStore.dispatch(routeIntent) // 发送路由请求

```

