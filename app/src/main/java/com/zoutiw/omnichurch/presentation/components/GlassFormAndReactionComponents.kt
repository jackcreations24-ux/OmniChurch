package com.zoutiw.omnichurch.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.data.models.OmniReactionType
import com.zoutiw.omnichurch.data.models.TargetReactionSummary
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PrimaryNavy
import com.zoutiw.omnichurch.ui.theme.PureWhite

@Composable
fun SectionTitle(
  title: String,
  modifier: Modifier = Modifier,
) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleSmall,
    color = BrandGold,
    fontWeight = FontWeight.Bold,
    modifier = modifier.padding(top = 4.dp, bottom = 2.dp),
  )
}

@Composable
fun CategoryChip(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(12.dp)
  Box(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .clip(shape)
        .background(
          if (isSelected) OmniBrushes.ActiveBlueNeonBrush
          else Brush.horizontalGradient(listOf(Color(0xFF132241), Color(0xFF0E1A33)))
        )
        .border(
          width = 1.dp,
          color = if (isSelected) BlueNeon else GlassWhite.copy(alpha = 0.22f),
          shape = shape,
        )
        .clickable(onClick = onClick)
        .padding(horizontal = 8.dp, vertical = 8.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = if (isSelected) PureWhite else GlassWhite,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
fun MinimalTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  placeholder: String = "",
  leadingIcon: ImageVector? = null,
  singleLine: Boolean = true,
  keyboardType: KeyboardType = KeyboardType.Text,
  testTag: String? = null,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    placeholder =
      if (placeholder.isNotBlank()) {
        { Text(text = placeholder, color = MutedGlassText.copy(alpha = 0.65f)) }
      } else null,
    leadingIcon =
      if (leadingIcon != null) {
        {
          Icon(
            imageVector = leadingIcon,
            contentDescription = label,
            tint = BlueNeon,
            modifier = Modifier.size(19.dp),
          )
        }
      } else null,
    singleLine = singleLine,
    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    shape = RoundedCornerShape(14.dp),
    colors =
      OutlinedTextFieldDefaults.colors(
        focusedTextColor = PureWhite,
        unfocusedTextColor = GlassWhite,
        focusedContainerColor = DeepNavy.copy(alpha = 0.72f),
        unfocusedContainerColor = DeepNavy.copy(alpha = 0.50f),
        focusedBorderColor = BlueNeon,
        unfocusedBorderColor = GlassWhite.copy(alpha = 0.22f),
        focusedLabelColor = BrandGold,
        unfocusedLabelColor = MutedGlassText,
        cursorColor = BlueNeon,
      ),
    modifier =
      modifier
        .fillMaxWidth()
        .let { m -> if (!testTag.isNullOrBlank()) m.testTag(testTag) else m },
  )
}

private fun OmniReactionType.drawableResId(): Int {
  return when (this) {
    OmniReactionType.LIKE -> R.drawable.reaction_like
    OmniReactionType.LOVE -> R.drawable.reaction_love
    OmniReactionType.PRAY -> R.drawable.reaction_pray
    OmniReactionType.HAHA -> R.drawable.reaction_haha
    OmniReactionType.WOW -> R.drawable.reaction_wow
    OmniReactionType.SAD -> R.drawable.reaction_sad
    OmniReactionType.CLAP -> R.drawable.reaction_clap
  }
}

@Composable
fun Floating3DReactionSelectorBar(
  visible: Boolean,
  selectedReaction: OmniReactionType?,
  onSelectReaction: (OmniReactionType) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn() + scaleIn(initialScale = 0.9f),
    exit = fadeOut() + scaleOut(targetScale = 0.9f),
    modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
  ) {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .shadow(12.dp, RoundedCornerShape(28.dp), ambientColor = ElectricBlue.copy(alpha = 0.4f))
          .clip(RoundedCornerShape(28.dp))
          .background(Brush.horizontalGradient(listOf(PrimaryNavy, Color(0xFF0E254D), PrimaryNavy)))
          .border(1.dp, BlueNeon.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
          .padding(horizontal = 8.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      OmniReactionType.entries.forEach { reaction ->
        val isSelected = selectedReaction == reaction
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier =
            Modifier.clip(RoundedCornerShape(14.dp))
              .background(if (isSelected) ElectricBlue.copy(alpha = 0.28f) else Color.Transparent)
              .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) BrandGold else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
              )
              .clickable {
                onSelectReaction(reaction)
                onDismiss()
              }
              .padding(horizontal = 6.dp, vertical = 4.dp),
        ) {
          Image(
            painter = painterResource(id = reaction.drawableResId()),
            contentDescription = reaction.labelHt,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(28.dp),
          )
          Text(
            text = reaction.labelHt,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = if (isSelected) BrandGold else GlassWhite,
            maxLines = 1,
          )
        }
      }
    }
  }
}

@Composable
fun Stacked3DReactionCluster(
  reactionSummary: TargetReactionSummary?,
  fallbackCount: Long = 0L,
  onTriggerClick: () -> Unit,
  modifier: Modifier = Modifier,
  showLabelWhenUnselected: String? = null,
  isCompact: Boolean = false,
  testTag: String? = null,
) {
  val totalCount =
    when {
      reactionSummary != null && reactionSummary.totalCount > 0 -> reactionSummary.totalCount.toLong()
      fallbackCount > 0L -> fallbackCount
      else -> 0L
    }
  val topReactions =
    reactionSummary?.topReactions?.takeIf { it.isNotEmpty() }
      ?: listOfNotNull(reactionSummary?.currentUserReaction)
  val iconSize = if (isCompact) 18.dp else 22.dp

  Row(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(50))
        .background(
          if (reactionSummary?.currentUserReaction != null) ElectricBlue.copy(alpha = 0.22f)
          else Color.Transparent
        )
        .border(
          width = if (reactionSummary?.currentUserReaction != null) 1.dp else 0.dp,
          color = if (reactionSummary?.currentUserReaction != null) BlueNeon.copy(alpha = 0.55f) else Color.Transparent,
          shape = RoundedCornerShape(50),
        )
        .clickable(onClick = onTriggerClick)
        .let { m -> if (!testTag.isNullOrBlank()) m.testTag(testTag) else m }
        .padding(horizontal = if (isCompact) 6.dp else 8.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (topReactions.isNotEmpty()) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        topReactions.take(3).forEachIndexed { idx, reaction ->
          Image(
            painter = painterResource(id = reaction.drawableResId()),
            contentDescription = reaction.labelHt,
            contentScale = ContentScale.Fit,
            modifier =
              Modifier.size(iconSize)
                .offset(x = (-4 * idx).dp)
                .zIndex((3 - idx).toFloat())
                .clip(CircleShape),
          )
        }
      }
    } else {
      Icon(
        imageVector = Icons.Default.ThumbUp,
        contentDescription = showLabelWhenUnselected ?: "Reyaji",
        tint = GlassWhite.copy(alpha = 0.85f),
        modifier = Modifier.size(if (isCompact) 16.dp else 18.dp),
      )
    }

    Spacer(modifier = Modifier.width(if (topReactions.size > 1) 2.dp else 6.dp))

    val displayText =
      when {
        totalCount > 0L -> totalCount.toString()
        !showLabelWhenUnselected.isNullOrBlank() -> showLabelWhenUnselected
        else -> "0"
      }
    Text(
      text = displayText,
      style = if (isCompact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
      color = if (reactionSummary?.currentUserReaction != null) BrandGold else GlassWhite,
      fontWeight = FontWeight.SemiBold,
    )
  }
}
