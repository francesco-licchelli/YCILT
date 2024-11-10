package com.example.ycilt.auth

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ycilt.R

@Composable
fun SignupScreen(
	onSignupClick: (username: String, password: String) -> Unit,
	onLoginClick: () -> Unit,
) {

	val usernameState = remember { mutableStateOf("") }
	val passwordState = remember { mutableStateOf("") }

	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(24.dp)
			.verticalScroll(rememberScrollState()),
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Text(
			text = stringResource(id = R.string.title_1),
			fontSize = 20.sp,
			textAlign = TextAlign.Center
		)

		Spacer(modifier = Modifier.height(8.dp))

		Text(
			text = stringResource(id = R.string.title_2),
			fontSize = 20.sp,
			textAlign = TextAlign.Center
		)

		Spacer(modifier = Modifier.height(8.dp))

		Text(
			text = stringResource(id = R.string.title_3),
			fontSize = 20.sp,
			textAlign = TextAlign.Center
		)

		Spacer(modifier = Modifier.height(32.dp))


		Image(
			bitmap = BitmapFactory.decodeResource(
				LocalContext.current.resources,
				R.drawable.ic_logo
			).asImageBitmap(),
			contentDescription = "App Icon",
			modifier = Modifier.size(128.dp)
		)

		Spacer(modifier = Modifier.height(32.dp))

		Card(
			modifier = Modifier
				.fillMaxWidth()
				.padding(bottom = 16.dp),
			shape = RoundedCornerShape(8.dp),
		) {
			Row(
				modifier = Modifier
					.padding(8.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				Icon(
					painter = painterResource(id = R.drawable.ic_user),
					contentDescription = stringResource(id = R.string.username_icon),
					modifier = Modifier.size(24.dp)
				)
				Spacer(modifier = Modifier.width(8.dp))
				TextField(
					value = usernameState.value,
					onValueChange = { usernameState.value = it },
					modifier = Modifier.fillMaxWidth(),
					placeholder = { Text(stringResource(id = R.string.username)) },
					singleLine = true,
					keyboardOptions = KeyboardOptions.Default.copy(
						imeAction = ImeAction.Next
					),
				)
			}
		}

		Card(
			modifier = Modifier
				.fillMaxWidth()
				.padding(bottom = 24.dp),
			shape = RoundedCornerShape(8.dp),
		) {
			Row(
				modifier = Modifier
					.padding(8.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				Icon(
					painter = painterResource(id = R.drawable.ic_password),
					contentDescription = stringResource(id = R.string.password_icon),
					modifier = Modifier.size(24.dp)
				)
				Spacer(modifier = Modifier.width(8.dp))
				TextField(
					value = passwordState.value,
					onValueChange = { passwordState.value = it },
					modifier = Modifier.fillMaxWidth(),
					placeholder = { Text(stringResource(id = R.string.password)) },
					singleLine = true,
					visualTransformation = PasswordVisualTransformation(),
					keyboardOptions = KeyboardOptions.Default.copy(
						imeAction = ImeAction.Done
					),
				)
			}
		}


		Button(
			onClick = { onSignupClick(usernameState.value, passwordState.value) },
			modifier = Modifier
				.fillMaxWidth()
				.padding(top = 16.dp),
			colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
		) {
			Text(
				text = stringResource(id = R.string.signup),
				color = Color.White
			)
		}

		Button(
			onClick = onLoginClick,
			modifier = Modifier
				.fillMaxWidth()
				.padding(top = 16.dp),
			colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
		) {
			Text(
				text = stringResource(id = R.string.already_have_an_account_login),
				color = Color.White
			)
		}

	}
}


