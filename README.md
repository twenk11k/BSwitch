# BSwitch
A customizable switch widget for Android<br><br>
![](bswitch.gif)<br>


### Usage
```xml
<com.twenk11k.bswitch.BSwitch
    android:id="@+id/bswitch"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:arc_padding="18"
    app:arc_stroke_width="2dp" />
```
#### More styles
```xml
<attr name="unchecked_color" format="reference|color" />
<attr name="checked_color" format="reference|color" />
<attr name="border_width" format="reference|dimension" />
<attr name="arc_stroke_width" format="reference|dimension" />
<attr name="arc_sweep_angle" format="reference|integer" />
<attr name="arc_padding" format="reference|integer" />
<attr name="draw_circle_enabled" format="reference|boolean" />
<attr name="duration" format="reference|integer" />
<attr name="is_checked" format="reference|boolean" />
```
