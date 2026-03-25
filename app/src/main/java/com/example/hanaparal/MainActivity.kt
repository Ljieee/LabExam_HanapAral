package com.example.hanaparal

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.hanaparal.data.model.StudyGroup
import com.example.hanaparal.data.remoteconfig.RemoteConfigManager
import com.example.hanaparal.data.repository.FirebaseRepository
import com.example.hanaparal.ui.auth.AuthViewModel
import com.example.hanaparal.ui.home.HomeViewModel
import com.example.hanaparal.ui.theme.HanapAralTheme

class MainActivity : FragmentActivity() {
    private val repository = FirebaseRepository()
    private val remoteConfigManager = RemoteConfigManager()
    private lateinit var authViewModel: AuthViewModel
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authViewModel = AuthViewModel(repository)
        homeViewModel = HomeViewModel(repository, remoteConfigManager)
        
        enableEdgeToEdge()
        setContent {
            HanapAralTheme {
                val navController = rememberNavController()
                val user by authViewModel.user.collectAsState()

                NavHost(navController = navController, startDestination = if (user == null) "login" else "home") {
                    composable("login") {
                        LoginScreen(onLoginSuccess = {
                            // In a real app, handle Google Sign-In here
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        })
                    }
                    composable("home") {
                        HomeScreen(homeViewModel, authViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("HanapAral", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onLoginSuccess) {
            Text("Sign in with Google")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(homeViewModel: HomeViewModel, authViewModel: AuthViewModel) {
    val groups by homeViewModel.groups.collectAsState()
    val announcement by homeViewModel.remoteConfigManager.announcementHeader.collectAsState()
    val isCreationEnabled by homeViewModel.remoteConfigManager.isGroupCreationEnabled.collectAsState()
    val userProfile by authViewModel.userProfile.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current as FragmentActivity

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(announcement) })
        },
        floatingActionButton = {
            if (isCreationEnabled) {
                FloatingActionButton(onClick = {
                    if (userProfile?.isSuperUser == true) {
                        showBiometricPrompt(context) {
                            showCreateDialog = true
                        }
                    } else {
                        showCreateDialog = true
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Create Group")
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(groups) { group ->
                GroupItem(group, onJoin = { homeViewModel.joinGroup(group.id) })
            }
        }
    }

    if (showCreateDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc ->
                homeViewModel.createGroup(name, desc)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun GroupItem(group: StudyGroup, onJoin: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(group.name, style = MaterialTheme.typography.titleLarge)
            Text(group.description)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onJoin) {
                Text("Join Group")
            }
        }
    }
}

@Composable
fun CreateGroupDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Study Group") },
        text = {
            Column {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                TextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(name, desc) }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun showBiometricPrompt(activity: FragmentActivity, onSucceed: () -> Unit) {
    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt = BiometricPrompt(activity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSucceed()
            }
        })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Biometric Authentication")
        .setSubtitle("Confirm to toggle features")
        .setNegativeButtonText("Cancel")
        .build()

    biometricPrompt.authenticate(promptInfo)
}
