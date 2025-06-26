package com.ppai.voicetotask.presentation.ui.paywall

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ppai.voicetotask.domain.model.PremiumFeature
import com.ppai.voicetotask.domain.model.SubscriptionType
import com.ppai.voicetotask.presentation.viewmodel.PaywallViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    onDismiss: () -> Unit,
    onSubscribed: () -> Unit,
    viewModel: PaywallViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var selectedPlan by remember { mutableStateOf(SubscriptionType.YEARLY) }
    var showWhyPremium by remember { mutableStateOf(false) }
    
    if (showWhyPremium) {
        WhyPremiumDialog(onDismiss = { showWhyPremium = false })
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1a1a2e),
                            Color(0xFF0f3460),
                            Color(0xFF16213e)
                        )
                    )
                )
        )
        
        // Close button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White.copy(alpha = 0.7f)
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))
            
            // Hero section
            PremiumHeroSection()
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Feature highlights
            FeatureHighlights()
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Pricing cards
            PricingSection(
                selectedPlan = selectedPlan,
                onPlanSelected = { selectedPlan = it }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Subscribe button
            SubscribeButton(
                selectedPlan = selectedPlan,
                isLoading = uiState.isProcessing,
                onClick = { 
                    viewModel.subscribe(context as android.app.Activity, selectedPlan)
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Why Premium button
            TextButton(
                onClick = { showWhyPremium = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Why Premium?",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Error handling
        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }
    }
    
    // Handle successful subscription
    LaunchedEffect(uiState.isSubscribed) {
        if (uiState.isSubscribed) {
            onSubscribed()
        }
    }
}

@Composable
private fun PremiumHeroSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Animated premium icon
        val infiniteTransition = rememberInfiniteTransition()
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            )
        )
        
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFD700),
                            Color(0xFFFFA500)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.WorkspacePremium,
                contentDescription = "Premium",
                modifier = Modifier.size(60.dp),
                tint = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Unlock Premium",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            ),
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Get the most out of Note By Voice",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FeatureHighlights() {
    val features = listOf(
        Triple(Icons.Default.Timer, "10-minute recordings", "vs 2 minutes"),
        Triple(Icons.Default.AllInclusive, "Unlimited notes", "vs 30/month"),
        Triple(Icons.Default.Block, "No ads", "Ad-free experience")
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        features.forEach { (icon, title, subtitle) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFFFFD700)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PricingSection(
    selectedPlan: SubscriptionType,
    onPlanSelected: (SubscriptionType) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Yearly plan (recommended)
        PricingCard(
            isSelected = selectedPlan == SubscriptionType.YEARLY,
            isRecommended = true,
            title = "Yearly",
            price = "$39.99",
            period = "per year",
            savings = "Save 33%",
            onClick = { onPlanSelected(SubscriptionType.YEARLY) }
        )
        
        // Monthly plan
        PricingCard(
            isSelected = selectedPlan == SubscriptionType.MONTHLY,
            isRecommended = false,
            title = "Monthly",
            price = "$4.99",
            period = "per month",
            onClick = { onPlanSelected(SubscriptionType.MONTHLY) }
        )
    }
}

@Composable
private fun PricingCard(
    isSelected: Boolean,
    isRecommended: Boolean,
    title: String,
    price: String,
    period: String,
    savings: String? = null,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.2f)
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                Color(0xFFFFD700).copy(alpha = 0.1f) 
            else 
                Color.White.copy(alpha = 0.05f)
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            price,
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (isSelected) Color(0xFFFFD700) else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            period,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                
                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFFFFD700),
                        unselectedColor = Color.White.copy(alpha = 0.5f)
                    )
                )
            }
            
            if (isRecommended) {
                Surface(
                    color = Color(0xFFFFD700),
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 0.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        "BEST VALUE",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            savings?.let {
                Text(
                    it,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
private fun SubscribeButton(
    selectedPlan: SubscriptionType,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFD700)
        ),
        shape = RoundedCornerShape(28.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.Black
            )
        } else {
            Text(
                "Start 7-Day Free Trial",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
    
    Text(
        "Cancel anytime. Renews at ${selectedPlan.price} after trial.",
        modifier = Modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.5f),
        textAlign = TextAlign.Center
    )
}