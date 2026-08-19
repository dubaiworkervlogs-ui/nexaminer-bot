package com.gamestudio.idlecoinrush

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import org.json.JSONObject
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    // Gameplay State
    private var coins: Double = 0.0
    private var tapPower: Double = 1.0
    private var autoMinerRate: Double = 0.0
    
    private var tapUpgradeLevel: Int = 1
    private var autoUpgradeLevel: Int = 0
    private var playerLevel: Int = 1
    private var totalTaps: Long = 0
    
    private var multiplier: Double = 1.0
    private var lastSaveTime: Long = System.currentTimeMillis()
    private var lastDailyClaim: Long = 0

    // UI Elements
    private lateinit var tvCoins: TextView
    private lateinit var tvTapPower: TextView
    private lateinit var tvAutoIncome: TextView
    private lateinit var tvPlayerLevel: TextView
    private lateinit var btnTap: Button
    
    // Upgrades UI
    private lateinit var btnUpgradeTap: Button
    private lateinit var btnUpgradeAuto: Button
    
    // Navigation & Screens
    private lateinit var layoutMain: View
    private lateinit var layoutUpgrades: View
    private lateinit var layoutRewards: View
    private lateinit var layoutStats: View
    private lateinit var layoutSettings: View
    
    // Stats UI
    private lateinit var tvStatTotalTaps: TextView
    private lateinit var tvStatMultiplier: TextView

    // Ads
    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null

    // Game Loop Handler
    private val handler = Handler(Looper.getMainLooper())
    private val gameLoop = object : Runnable {
        override fun run() {
            addCoins((autoMinerRate * multiplier) / 10.0)
            updateUI()
            handler.postDelayed(this, 100) // 10 ticks per second
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        loadGameState()
        calculateOfflineEarnings()
        initAds()

        handler.post(gameLoop)
    }

    private fun initViews() {
        tvCoins = findViewById(R.id.tvCoins)
        tvTapPower = findViewById(R.id.tvTapPower)
        tvAutoIncome = findViewById(R.id.tvAutoIncome)
        tvPlayerLevel = findViewById(R.id.tvPlayerLevel)
        btnTap = findViewById(R.id.btnTap)
        
        btnUpgradeTap = findViewById(R.id.btnUpgradeTap)
        btnUpgradeAuto = findViewById(R.id.btnUpgradeAuto)
        
        layoutMain = findViewById(R.id.layoutMain)
        layoutUpgrades = findViewById(R.id.layoutUpgrades)
        layoutRewards = findViewById(R.id.layoutRewards)
        layoutStats = findViewById(R.id.layoutStats)
        layoutSettings = findViewById(R.id.layoutSettings)

        tvStatTotalTaps = findViewById(R.id.tvStatTotalTaps)
        tvStatMultiplier = findViewById(R.id.tvStatMultiplier)

        btnTap.setOnClickListener {
            addCoins(tapPower * multiplier)
            totalTaps++
            checkLevelProgression()
            updateUI()
        }

        btnUpgradeTap.setOnClickListener {
            val cost = getTapUpgradeCost()
            if (coins >= cost) {
                coins -= cost
                tapUpgradeLevel++
                tapPower += 1.5
                updateUI()
            } else {
                Toast.makeText(this, "Not enough coins!", Toast.LENGTH_SHORT).show()
            }
        }

        btnUpgradeAuto.setOnClickListener {
            val cost = getAutoUpgradeCost()
            if (coins >= cost) {
                coins -= cost
                autoUpgradeLevel++
                autoMinerRate += 2.0
                updateUI()
            } else {
                Toast.makeText(this, "Not enough coins!", Toast.LENGTH_SHORT).show()
            }
        }

        // Tab Navigation Listeners
        findViewById<View>(R.id.navMain).setOnClickListener { showScreen(layoutMain) }
        findViewById<View>(R.id.navUpgrades).setOnClickListener { showScreen(layoutUpgrades) }
        findViewById<View>(R.id.navRewards).setOnClickListener { showScreen(layoutRewards) }
        findViewById<View>(R.id.navStats).setOnClickListener { showScreen(layoutStats) }
        findViewById<View>(R.id.navSettings).setOnClickListener { showScreen(layoutSettings) }

        // Daily Reward & Ads Buttons
        findViewById<Button>(R.id.btnDailyReward).setOnClickListener { claimDailyReward() }
        findViewById<Button>(R.id.btnWatchAdBoost).setOnClickListener { showRewardedAd() }
    }

    private fun addCoins(amount: Double) {
        if (amount > 0) {
            coins += amount
        }
    }

    private fun getTapUpgradeCost(): Double = 10.0 * (1.5.pow(tapUpgradeLevel - 1))
    private fun getAutoUpgradeCost(): Double = 25.0 * (1.6.pow(autoUpgradeLevel))

    private fun checkLevelProgression() {
        val requiredTaps = playerLevel * 50
        if (totalTaps >= requiredTaps) {
            playerLevel++
            Toast.makeText(this, "Level Up! You reached Level $playerLevel", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showScreen(target: View) {
        layoutMain.visibility = View.GONE
        layoutUpgrades.visibility = View.GONE
        layoutRewards.visibility = View.GONE
        layoutStats.visibility = View.GONE
        layoutSettings.visibility = View.GONE
        target.visibility = View.VISIBLE
    }

    private fun updateUI() {
        tvCoins.text = String.format("%.1f Coins", coins)
        tvTapPower.text = String.format("Tap Power: %.1f", tapPower * multiplier)
        tvAutoIncome.text = String.format("Auto Rate: %.1f/s", autoMinerRate * multiplier)
        tvPlayerLevel.text = "Level: $playerLevel"

        btnUpgradeTap.text = String.format("Upgrade Tap (Lvl %d)\nCost: %.1f", tapUpgradeLevel, getTapUpgradeCost())
        btnUpgradeAuto.text = String.format("Upgrade Auto (Lvl %d)\nCost: %.1f", autoUpgradeLevel, getAutoUpgradeCost())

        tvStatTotalTaps.text = "Total Taps: $totalTaps"
        tvStatMultiplier.text = String.format("Current Multiplier: %.1fx", multiplier)
    }

    private fun claimDailyReward() {
        val currentTime = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000
        if (currentTime - lastDailyClaim >= oneDayMillis) {
            val reward = 100.0 * playerLevel
            addCoins(reward)
            lastDailyClaim = currentTime
            Toast.makeText(this, "Claimed $reward daily coins!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Daily reward available tomorrow!", Toast.LENGTH_SHORT).show()
        }
    }

    // Offline Income Engine (8 Hours Cap)
    private fun calculateOfflineEarnings() {
        val currentTime = System.currentTimeMillis()
        val elapsedSeconds = (currentTime - lastSaveTime) / 1000
        if (elapsedSeconds > 10 && autoMinerRate > 0) {
            val maxCapSeconds = 8 * 3600 // 8-hour capping
            val activeSeconds = Math.min(elapsedSeconds, maxCapSeconds.toLong())
            val offlineGain = activeSeconds * autoMinerRate * multiplier
            addCoins(offlineGain)
            Toast.makeText(this, String.format("Welcome back! Earned %.1f offline coins.", offlineGain), Toast.LENGTH_LONG).show()
        }
    }

    // Save/Load System
    private fun saveGameState() {
        val prefs: SharedPreferences = getSharedPreferences("IdleCoinRushPrefs", Context.MODE_PRIVATE)
        val data = JSONObject().apply {
            put("coins", coins)
            put("tapPower", tapPower)
            put("autoMinerRate", autoMinerRate)
            put("tapUpgradeLevel", tapUpgradeLevel)
            put("autoUpgradeLevel", autoUpgradeLevel)
            put("playerLevel", playerLevel)
            put("totalTaps", totalTaps)
            put("lastSaveTime", System.currentTimeMillis())
            put("lastDailyClaim", lastDailyClaim)
        }
        prefs.edit().putString("saveData", data.toString()).apply()
    }

    private fun loadGameState() {
        val prefs: SharedPreferences = getSharedPreferences("IdleCoinRushPrefs", Context.MODE_PRIVATE)
        val jsonString = prefs.getString("saveData", null) ?: return
        try {
            val data = JSONObject(jsonString)
            coins = data.optDouble("coins", 0.0)
            tapPower = data.optDouble("tapPower", 1.0)
            autoMinerRate = data.optDouble("autoMinerRate", 0.0)
            tapUpgradeLevel = data.optInt("tapUpgradeLevel", 1)
            autoUpgradeLevel = data.optInt("autoUpgradeLevel", 0)
            playerLevel = data.optInt("playerLevel", 1)
            totalTaps = data.optLong("totalTaps", 0)
            lastSaveTime = data.optLong("lastSaveTime", System.currentTimeMillis())
            lastDailyClaim = data.optLong("lastDailyClaim", 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // AdMob Safe Setup
    private fun initAds() {
        MobileAds.initialize(this) {}
        loadRewardedAd()
        loadInterstitialAd()
    }

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        // Google official Test Rewarded Ad Unit ID
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad }
            override fun onAdFailedToLoad(error: LoadAdError) { rewardedAd = null }
        })
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        // Google official Test Interstitial Ad Unit ID
        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad }
            override fun onAdFailedToLoad(error: LoadAdError) { interstitialAd = null }
        })
    }

    private fun showRewardedAd() {
        if (rewardedAd != null) {
            rewardedAd?.show(this) {
                multiplier = 2.0 // Temporary 2x Multiplier
                Toast.makeText(this, "2x Boost Activated for this session!", Toast.LENGTH_SHORT).show()
                handler.postDelayed({ multiplier = 1.0 }, 120000) // 2 Min Duration
            }
        } else {
            Toast.makeText(this, "Ad is loading, try again soon!", Toast.LENGTH_SHORT).show()
            loadRewardedAd()
        }
    }

    override fun onPause() {
        super.onPause()
        saveGameState()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(gameLoop)
        saveGameState()
    }
}
