package br.edu.utfpr.rodada.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.edu.utfpr.rodada.sensor.MonitorService
import br.edu.utfpr.rodada.sensor.MonitorState
import br.edu.utfpr.rodada.ui.screens.CreateGroupSheet
import br.edu.utfpr.rodada.ui.screens.GroupDetailScreen
import br.edu.utfpr.rodada.ui.screens.GroupsScreen
import br.edu.utfpr.rodada.ui.screens.JoinWithCodeSheet
import br.edu.utfpr.rodada.ui.screens.PermissionsSheet
import br.edu.utfpr.rodada.ui.screens.RankingScreen
import br.edu.utfpr.rodada.ui.screens.TodayScreen
import br.edu.utfpr.rodada.ui.screens.WorkoutScreen
import br.edu.utfpr.rodada.ui.screens.WorkoutsScreen
import br.edu.utfpr.rodada.ui.screens.rememberPhoto

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Today("today", "Hoje", Icons.Rounded.Bolt),
    Workouts("workouts", "Atividades", Icons.Rounded.Route),
    Groups("groups", "Grupos", Icons.Rounded.Groups),
    Ranking("ranking", "Ranking", Icons.Rounded.EmojiEvents),
}

private enum class OpenSheet { None, Permissions, Create, Join }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RodadaApp(vm: HomeViewModel, onSignOut: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val snackbar = remember { SnackbarHostState() }
    var sheet by rememberSaveable { mutableStateOf(OpenSheet.None) }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    // When a session ends, open its summary (route animation + photo).
    LaunchedEffect(ui.monitor.finishedWorkoutId) {
        val id = ui.monitor.finishedWorkoutId ?: return@LaunchedEffect
        MonitorState.consumeFinishedWorkout()
        nav.navigate("workout/$id?finished=true")
    }
    val photoVersion by vm.photoVersion.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        // Starts even if permissions were denied: the accelerometer needs none.
        MonitorService.start(context)
    }

    fun startActivity() {
        val needed = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(Manifest.permission.ACTIVITY_RECOGNITION)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        permissionLauncher.launch(needed.toTypedArray())
    }

    fun toggleActivity() {
        when {
            ui.monitor.active -> MonitorService.stop(context)
            !vm.permissionsExplained -> sheet = OpenSheet.Permissions
            else -> startActivity()
        }
    }

    fun goTo(tab: Tab) = nav.navigate(tab.route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    val currentTab = Tab.entries.firstOrNull { it.route == route }
    val barColors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        scrolledContainerColor = MaterialTheme.colorScheme.surface,
    )

    Scaffold(
        topBar = {
            when {
                route == Tab.Today.route || route == null -> Unit // Today draws its own header.
                route.startsWith("workout/") -> TopAppBar(
                    title = { Text("Atividade", style = MaterialTheme.typography.headlineSmall) },
                    navigationIcon = {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                    colors = barColors,
                )
                route.startsWith("group/") -> {
                    val group = ui.groups.firstOrNull { it.id == entry?.arguments?.getString("id") }
                    TopAppBar(
                        title = { Text(group?.name ?: "Grupo", style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        navigationIcon = {
                            IconButton(onClick = { nav.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                            }
                        },
                        colors = barColors,
                    )
                }
                else -> TopAppBar(
                    title = {
                        Text(
                            when (currentTab) {
                                Tab.Ranking -> "Ranking geral"
                                Tab.Workouts -> "Suas atividades"
                                else -> "Seus grupos"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    },
                    actions = {
                        if (currentTab == Tab.Groups) {
                            TextButton(onClick = { sheet = OpenSheet.Join }) {
                                Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Entrar com código")
                            }
                        }
                    },
                    colors = barColors,
                )
            }
        },
        bottomBar = {
            if (currentTab != null) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == currentTab,
                            onClick = { goTo(tab) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentTab == Tab.Groups) {
                ExtendedFloatingActionButton(
                    onClick = { sheet = OpenSheet.Create },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Criar grupo", style = MaterialTheme.typography.labelLarge) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        NavHost(nav, startDestination = Tab.Today.route) {
            composable(Tab.Today.route) {
                TodayScreen(
                    ui = ui,
                    contentPadding = padding,
                    onToggleActivity = ::toggleActivity,
                    onFocusGroup = vm::focus,
                    onOpenGroup = { nav.navigate("group/${it.id}") },
                    onCreateGroup = { sheet = OpenSheet.Create },
                    onJoinWithCode = { sheet = OpenSheet.Join },
                    onSignOut = { MonitorService.stop(context); onSignOut() },
                )
            }
            composable(Tab.Workouts.route) {
                WorkoutsScreen(
                    workouts = ui.workouts,
                    photoVersion = photoVersion,
                    loadPhoto = vm::loadPhoto,
                    contentPadding = padding,
                    onOpen = { nav.navigate("workout/${it.id}") },
                )
            }
            composable(
                "workout/{id}?finished={finished}",
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("finished") { type = NavType.BoolType; defaultValue = false },
                ),
            ) { e ->
                val id = e.arguments?.getString("id").orEmpty()
                val workout = ui.workouts.firstOrNull { it.id == id }
                if (workout != null) {
                    val photo = rememberPhoto(id, photoVersion, vm::loadPhoto)
                    WorkoutScreen(
                        workout = workout,
                        justFinished = e.arguments?.getBoolean("finished") == true,
                        photo = photo,
                        contentPadding = padding,
                        onSavePhoto = { vm.savePhoto(id, it) },
                        onRemovePhoto = { vm.removePhoto(id) },
                        onDone = { nav.popBackStack() },
                    )
                }
            }
            composable(Tab.Groups.route) {
                GroupsScreen(
                    groups = ui.groups,
                    loading = ui.loading,
                    contentPadding = padding,
                    onOpen = { nav.navigate("group/${it.id}") },
                    onJoinWithCode = { sheet = OpenSheet.Join },
                )
            }
            composable(Tab.Ranking.route) {
                RankingScreen(ui.rankingToday, ui.rankingTotal, ui.user.uid, padding)
            }
            composable("group/{id}") { e ->
                val id = e.arguments?.getString("id")
                val group = ui.groups.firstOrNull { it.id == id }
                if (group != null) {
                    LaunchedEffect(id) { vm.focus(group.id) }
                    GroupDetailScreen(group, if (ui.group?.id == group.id) ui.standings else emptyList(), ui.user.uid, padding)
                }
            }
        }
    }

    when (sheet) {
        OpenSheet.Permissions -> PermissionsSheet(
            onContinue = { vm.markPermissionsExplained(); sheet = OpenSheet.None; startActivity() },
            onDismiss = { sheet = OpenSheet.None },
        )
        OpenSheet.Create -> CreateGroupSheet(
            onCreate = { name, days, points, steps ->
                vm.createGroup(name, days, points, steps) { g ->
                    sheet = OpenSheet.None
                    nav.navigate("group/${g.id}")
                }
            },
            onDismiss = { sheet = OpenSheet.None },
        )
        OpenSheet.Join -> JoinWithCodeSheet(
            onJoin = { code, onError ->
                vm.joinGroup(code, onJoined = { sheet = OpenSheet.None; goTo(Tab.Today) }, onError = onError)
            },
            onDismiss = { sheet = OpenSheet.None },
        )
        OpenSheet.None -> Unit
    }
}
