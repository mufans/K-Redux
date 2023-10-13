package com.mufans.mvi

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.modifier.modifierLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mufans.mvi.login.LoginEvent
import com.mufans.mvi.login.LoginIntent
import com.mufans.mvi.login.LoginViewModel
import com.mufans.mvi.ui.theme.KReduxTheme
import kotlinx.coroutines.flow.collect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KReduxTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    MainPage()
                }
            }
        }
    }
}


@Composable
fun MainPage(viewModel: LoginViewModel = viewModel()) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.singleEvent.collect {
            if (it is LoginEvent.Failure) {
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
            } else if (it is LoginEvent.Success) {
                Toast.makeText(context, "login success", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val loginState = viewModel.stateFlow.collectAsState()
    if (loginState.value.token.isNullOrEmpty()) {
        LoginPage { user, pwd ->
            viewModel.dispatch(
                LoginIntent.RequestLogin(user, pwd)
            )
        }
    } else {
        LoginSuccessPage(userName = loginState.value.name ?: "") {
            viewModel.dispatch(LoginIntent.Logout)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginPage(onClick: (String, String) -> Unit) {
    val userName = remember {
        mutableStateOf("")
    }
    val pass = remember {
        mutableStateOf("")
    }
    Column(modifier = Modifier.padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "username:", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(10.dp))
            TextField(
                value = userName.value,
                colors = TextFieldDefaults.textFieldColors(containerColor = Color.Transparent),
                onValueChange = { value -> userName.value = value })
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "password:", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(10.dp))
            TextField(
                value = pass.value,
                colors = TextFieldDefaults.textFieldColors(containerColor = Color.Transparent),
                onValueChange = { value -> pass.value = value })
        }
        Spacer(modifier = Modifier.height(30.dp))
        Button(
            onClick = { onClick(userName.value, pass.value) },
            modifier = Modifier
                .background(color = Color.Blue)
                .fillMaxWidth(),
            enabled = userName.value.isNotEmpty() && pass.value.isNotEmpty()
        ) {
            Text(
                text = "login",
                fontSize = 18.sp,
                style = TextStyle(color = Color.White)
            )
        }
    }
}

@Composable
fun LoginSuccessPage(userName: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Welcome , user $userName!!", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(50.dp))
        Button(
            onClick = onClick,
            modifier = Modifier
                .background(color = Color.Blue)
                .fillMaxWidth(),
        ) {
            Text(
                text = "logout",
                fontSize = 18.sp,
                style = TextStyle(color = Color.White)
            )
        }
    }
}