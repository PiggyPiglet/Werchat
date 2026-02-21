# CustomUI Snippets

## Color Picker Dropdown

```ui
ColorPickerDropdownBox {
  Anchor: (Width: 32, Height: 32);
  Style: $C.@DefaultColorPickerDropdownBoxStyle;
  Color: #ff5555;
}
```

## Dropdown Box

```ui
$C.@DropdownBox {
  @Anchor = (Width: 250);
  Value: "Option_2";

  DropdownEntry {
    Value: "Option_1";
    Text: "Option 1";
  }
  // ... more entries
}
```

## CheckBox With Label

```ui
$C.@CheckBoxWithLabel {
  @Text = "Enable feature";
  @Checked = true;
}
```

## CheckBox

```ui
$C.@CheckBox {
  Value: true;  // or false
}
```

## Tab Navigation

```ui
TabNavigation {
  Style: $C.@TopTabsStyle;
  SelectedTab: "Tab1";
  Anchor: (Height: 66, Left: 2, Right: 0);

  TabButton {
    Icon: "icon.png";
    TooltipText: "Tab One";
    Id: "Tab1";
  }
  // more tabs...
}
```

## Top Scrolling Group

```ui
Group {
  LayoutMode: TopScrolling;
  ScrollbarStyle: $C.@DefaultScrollbarStyle;
  // content here
}
```

## Extra Spacing Scrollbar Style

```ui
ScrollbarStyle: $C.@DefaultExtraSpacingScrollbarStyle;
// 12px spacing from content edge
```

## Progress Bar

```ui
$C.@ProgressBar #ProgressBar75 {
  @Anchor = (Bottom: 4, Left: 0);
  Value: 0.75;
}
```

## Circular Progress Bar

```ui
CircularProgressBar {
  Anchor: (Width: 48, Height: 48);
  Value: 0.66;
  Background: #1a2030;
  Color: #4a7caa;
}
```

## Spinner

```ui
$C.@DefaultSpinner {}

// Or customize:
Sprite {
  TexturePath: "Common/Spinner.png";
  Frame: (Width: 32, Height: 32,
          PerRow: 8, Count: 72);
  FramesPerSecond: 30;
}
```

## Slider

```ui
$C.@Slider {
  @Anchor = (Width: 300);
  Value: 50;
  Min: 0;
  Max: 100;
}
```

## Float Slider

```ui
$C.@FloatSlider {
  @Anchor = (Width: 300);
  Value: 0.5;
  Min: 0.0;
  Max: 1.0;
}
```

## Slider Number Field

```ui
$C.@SliderNumberField {
  @Anchor = (Width: 380);
  Value: 50;
  Min: 0;
  Max: 100;
  NumberFieldContainerAnchor: (Width: 60);
}
```

## Title And Subtitle

```ui
$C.@Title { @Text = "Header"; }
$C.@Subtitle { @Text = "Subtitle"; }
```

## Default Label Text Color Style

```ui
Style: (TextColor: $C.@ColorDefaultLabel)
```

## Gradient Text Label

```ui
// Wrap in Group with LayoutMode: Left to fit text bounds
Group {
  LayoutMode: Left;

  Label {
    Text: "Gradient Text";
    Style: (FontSize: 24, TextColor: $C.@ColorDefault, RenderBold: true);
    MaskTexturePath: $C.@TextHighlightGradientMask;
  }
}
```

## Panel

```ui
$C.@Panel {
  Anchor: (Width: 350, Height: 80);
  Padding: (Full: 15);
  // content here
}
```

## Panel Title

```ui
$C.@PanelTitle {
  @Text = "Panel Title";
}
```

## Container

```ui
$C.@Container {
  Anchor: (Width: 400, Height: 150);

  #Title {
    $C.@Title { @Text = "Title"; }
  }

  #Content {
    // content here
  }
}
```

## Decorated Container

```ui
$C.@DecoratedContainer {
  Anchor: (Width: 400, Height: 170);
  // Same structure as @Container
}
```

## Top Tabs With Header Tabs And Search

```ui
// Top tabs above container
TabNavigation {
  Anchor: (Height: 66, Left: 2, Right: 0);
  Style: $C.@TopTabsStyle;
  // TabButtons...
}

$C.@Container {
  #Title {
    Group {
      LayoutMode: Left;

      // Header tabs (left)
      TabNavigation {
        Anchor: (Height: 34, Left: 4);
        Style: $C.@HeaderTabsStyle;
      }

      Group { FlexWeight: 1; } // Spacer

      // Search (right)
      $C.@HeaderSearch #Search {}
    }
  }

  #Content { ... }
}
```

## Simple Container

```ui
$C.@SimpleContainer {
  Anchor: (Width: 350, Height: 80);
  // content here
}
```

## Separators

```ui
$C.@ContentSeparator {}
$C.@PanelSeparatorFancy {}
```

## Text Field

```ui
$C.@TextField {
  @Anchor = (Width: 300);
  PlaceholderText: "Enter text here...";
}
```

## Number Field

```ui
$C.@NumberField {
  @Anchor = (Width: 150);
  Value: 10;
}
```

## Compact Text Field

```ui
CompactTextField {
  CollapsedWidth: 34;
  ExpandedWidth: 200;
  PlaceholderText: "Search...";
  Decoration: (
    Default: (
      Icon: (Texture: "SearchIcon.png", ...)
    )
  );
}
```

## Multiline Text Field

```ui
$C.@MultilineTextField {
  @Anchor = (Width: 400, Height: 80, Bottom: 8, Left: 0);
  PlaceholderText: "Enter text...";
}
```

## Text Button With Tooltip

```ui
$C.@TextButton {
  @Text = "Hover Me";
  TooltipText: "Tooltip text here";
  TextTooltipStyle: $C.@DefaultTextTooltipStyle;
}
```

## Tooltip Usage

```ui
// Add to any element:
TooltipText: "Your tooltip";
TextTooltipStyle: $C.@DefaultTextTooltipStyle;

// For localized text:
TooltipTextSpans: %key.to.translation;
```
