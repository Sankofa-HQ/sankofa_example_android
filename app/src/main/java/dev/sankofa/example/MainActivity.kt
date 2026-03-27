package dev.sankofa.example

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dev.sankofa.sdk.Sankofa

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnTrackPurchase).setOnClickListener {
            Sankofa.track(
                "purchase_completed",
                mapOf(
                    "item" to "Premium Plan",
                    "price" to 99.99,
                    "currency" to "USD"
                )
            )
            Toast.makeText(this, "Tracked 'purchase_completed'", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnIdentifyUser).setOnClickListener {
            Sankofa.identify("user_prod_778899")
            Toast.makeText(this, "Identified user_prod_778899", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnSetPerson).setOnClickListener {
            Sankofa.setPerson(
                name = "Samuel Asare",
                email = "samuel@sankofa.dev"
            )
            Toast.makeText(this, "Person profile updated", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnReset).setOnClickListener {
            Sankofa.reset()
            Toast.makeText(this, "Identity reset (Anonymous)", Toast.LENGTH_SHORT).show()
        }
    }
}
