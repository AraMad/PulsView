# PulsView
Simple puls line view for Android

![puls view by arastark](https://media.giphy.com/media/eivwqATrtL7CTrPMrE/giphy.gif)

##Usage

```
<com.orynastark.pulsview.PulsView

            android:id="@+id/arrowView"

            android:layout_width="match_parent"
            android:layout_height="match_parent"

            app:viewFrom="@id/secondItem"
            app:viewTo="@id/firstItem"

            app:corners="50dp"
            app:lineColor="@color/colorAccent"
            app:isMirrored="false"
            app:bypass="0dp"
            app:lineWidth="2dp"

            app:layout_constraintTop_toBottomOf="@id/fourthItem"
            app:layout_constraintBottom_toTopOf="@id/secondItem"
    />
```
