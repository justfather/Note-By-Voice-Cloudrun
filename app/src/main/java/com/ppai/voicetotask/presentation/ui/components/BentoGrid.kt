package com.ppai.voicetotask.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BentoGrid(
    modifier: Modifier = Modifier,
    columns: Int = 2,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    verticalSpacing: androidx.compose.ui.unit.Dp = 16.dp,
    horizontalSpacing: androidx.compose.ui.unit.Dp = 16.dp,
    content: @Composable BentoGridScope.() -> Unit
) {
    val scope = BentoGridScopeImpl()
    scope.content()
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        var currentRow = mutableListOf<BentoGridItem>()
        var currentRowSpan = 0
        
        scope.items.forEach { item ->
            if (currentRowSpan + item.span > columns) {
                // Render current row
                if (currentRow.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
                    ) {
                        currentRow.forEach { rowItem ->
                            Box(
                                modifier = Modifier
                                    .weight(rowItem.span.toFloat())
                                    .then(rowItem.modifier)
                            ) {
                                rowItem.content()
                            }
                        }
                        // Fill remaining space if needed
                        val remainingSpan = columns - currentRow.sumOf { it.span }
                        if (remainingSpan > 0) {
                            Spacer(modifier = Modifier.weight(remainingSpan.toFloat()))
                        }
                    }
                }
                currentRow = mutableListOf(item)
                currentRowSpan = item.span
            } else {
                currentRow.add(item)
                currentRowSpan += item.span
            }
        }
        
        // Render last row
        if (currentRow.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
            ) {
                currentRow.forEach { rowItem ->
                    Box(
                        modifier = Modifier
                            .weight(rowItem.span.toFloat())
                            .then(rowItem.modifier)
                    ) {
                        rowItem.content()
                    }
                }
                // Fill remaining space if needed
                val remainingSpan = columns - currentRow.sumOf { it.span }
                if (remainingSpan > 0) {
                    Spacer(modifier = Modifier.weight(remainingSpan.toFloat()))
                }
            }
        }
    }
}

interface BentoGridScope {
    fun item(
        span: Int = 1,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    )
}

private class BentoGridScopeImpl : BentoGridScope {
    val items = mutableListOf<BentoGridItem>()
    
    override fun item(
        span: Int,
        modifier: Modifier,
        content: @Composable () -> Unit
    ) {
        items.add(BentoGridItem(span, modifier, content))
    }
}

private data class BentoGridItem(
    val span: Int,
    val modifier: Modifier,
    val content: @Composable () -> Unit
)