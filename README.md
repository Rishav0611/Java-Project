

























Readme · MD
# AI Privacy Shield

A Java tool that **hides your personal data before you send text to an AI chatbot**, and **puts it back** in the AI's reply.

> You type: `My phone is 9876543210`
> The AI sees: `My phone is [PHONE_1]`
> The AI replies: `Okay, I will call [PHONE_1]`
> You read: `Okay, I will call 9876543210`

The AI never sees your real phone number, email, PAN or Aadhaar.
 
---

## Table of contents

1. [Why this project exists](#1-why-this-project-exists)
2. [Current status](#2-current-status)
3. [How to run it](#3-how-to-run-it)
4. [The big picture](#4-the-big-picture)
5. [Project structure](#5-project-structure)
6. [Step-by-step: how each part was built, and why](#6-step-by-step-how-each-part-was-built-and-why)
7. [Testing](#7-testing)
8. [Build it yourself from scratch (learning path)](#8-build-it-yourself-from-scratch-learning-path)
9. [Known limitations](#9-known-limitations)
10. [Roadmap](#10-roadmap)
11. [Glossary](#11-glossary)
12. [Team](#12-team)
---

## 1. Why this project exists

People now paste forms, resumes, emails and documents into AI tools. Those texts often contain private data such as phone numbers, email addresses, PAN and Aadhaar numbers. Once sent, you do not control where that data goes.

This project puts a **shield** in between:

1. Find the private data in your text.
2. Replace it with harmless placeholders (tokens).
3. Keep the real values in a **locked (encrypted) vault** on your own machine.
4. When the AI answers, swap the placeholders back to the real values.
---

## 2. Current status

| Part | Status |
|---|---|
| Detect phone, email, PAN, Aadhaar | Done |
| Resolve overlapping matches (engine) | Done |
| Mask text with tokens like `[PHONE_1]` | Done |
| Unmask AI replies (tolerates `PHONE_1`, `[phone 1]`) | Done |
| Encrypted vault with auto-expiry (in memory) | Done |
| Custom-word detector (names, company names) | Not yet |
| Vault saved to disk (SQLite) | Not yet |
| Desktop GUI (Swing) | Not yet |
| Photo metadata (EXIF) scan and strip | Not yet |
| OCR / QR redaction, clipboard shield | Not yet |

45 automated tests currently pass.
 
---

## 3. How to run it

**You need:** Java 17 and Maven. IntelliJ IDEA is recommended.

```bash
git clone https://github.com/Rishav0611/Java-Project.git
cd Java-Project
mvn test          # runs all tests
```

To see the demo, open `src/main/java/com/privacyshield/detect/Main.java` in IntelliJ and click the green Run button next to `main`.

Real output of the demo:

```
1. Original:
   Mera naam form me bharo. Phone 9876543210, email rahul@gmail.com, PAN ABCPD1234E, Aadhaar 2345 6789 0124.
 
2. What the AI will receive (masked):
   Mera naam form me bharo. Phone [PHONE_1], email [EMAIL_1], PAN [PAN_1], Aadhaar [AADHAAR_1].
 
3. Saved in the vault (encrypted, 24 hour expiry). Entries: 1
 
4. The AI's reply (note the messy token styles):
   Form ready: phone PHONE_1, email [email 1], PAN [PAN_1], Aadhaar [AADHAAR_1].
 
5. After unmasking, what the user sees:
   Form ready: phone 9876543210, email rahul@gmail.com, PAN ABCPD1234E, Aadhaar 2345 6789 0124.
```

Notice step 4: the AI wrote `PHONE_1` and `[email 1]` instead of `[PHONE_1]` and `[EMAIL_1]`, and it still worked. That is the job of the Unmasker.
 
---

## 4. The big picture

```
 Your text
    │
    ▼
 [1. DETECT]   find phone / email / PAN / Aadhaar          (package: detect)
    │
    ▼
 [2. MASK]     replace each one with [TYPE_n]               (package: mask)
    │                         │
    │                         ▼
    │              mapping { [PHONE_1] -> 9876543210 }
    │                         │
    │                         ▼
    │              [3. VAULT] encrypt and store it            (package: vault)
    ▼
 Send masked text to the AI
    │
    ▼
 AI reply (contains tokens)
    │
    ▼
 [4. UNMASK]   read mapping from vault, put real values back  (package: mask)
    │
    ▼
 Your final answer
```
 
---

## 5. Project structure

```
src/main/java/com/privacyshield/
├── detect/
│   ├── DataType.java          what kinds of data exist (enum)
│   ├── Detection.java         one finding: where, what type, which text (record)
│   ├── Detector.java          the rule every detector follows (interface)
│   ├── PhoneDetector.java     finds Indian mobile numbers
│   ├── EmailDetector.java     finds email addresses
│   ├── PanDetector.java       finds PAN numbers
│   ├── AadhaarDetector.java   finds Aadhaar numbers (with checksum)
│   ├── Verhoeff.java          the checksum maths used by Aadhaar
│   ├── DetectionEngine.java   runs all detectors, removes overlaps
│   └── Main.java              demo program
├── mask/
│   ├── MaskResult.java        masked text + mapping (record)
│   ├── Masker.java            real value -> token
│   └── Unmasker.java          token -> real value
└── vault/
    ├── CryptoService.java     encryption and decryption
    ├── RetentionPolicy.java   how long to keep data
    └── Vault.java             encrypted storage with expiry
 
src/test/java/com/privacyshield/   unit tests, same package layout
pom.xml                            Maven build file
```

**Why separate packages?** Each package does one job. `detect` knows nothing about encryption, and `vault` knows nothing about regex. If one part changes, the others do not break.
 
---

## 6. Step-by-step: how each part was built, and why

Read this section in order. Each step builds on the one before it.

### Step 0: Project setup (Maven)

**What:** Create a Maven project with Java 17 and add JUnit 5 for testing.

**Why:** Maven downloads libraries automatically and gives one standard way to build and test (`mvn test`). `pom.xml` is an XML file that describes the project: its name, Java version and dependencies.

**How:** In `pom.xml`, set `maven.compiler.source/target` to 17 and add the dependency `org.junit.jupiter:junit-jupiter:5.10.2` with scope `test`. After editing, click **Reload** in IntelliJ's Maven panel.

### Step 1: The vocabulary (`DataType`, `Detection`, `Detector`)

**What:** Three tiny files that define the language of the project.

**`DataType` (enum)**
```java
public enum DataType { AADHAAR, PAN, PHONE, EMAIL, CUSTOM }
```
An enum is a type with a *fixed* list of values. **Why not a String?** With `"PHOEN"` (typo) Java does not complain until something breaks later. With `DataType.PHOEN` the compiler complains immediately.

**`Detection` (record)**
```java
public record Detection(int start, int end, DataType type, String value) {}
```
One finding: "from position `start` up to (not including) `end`, I found a `type`, and the text was `value`". **Why store positions?** So that later we can replace exactly that part of the text and nothing else. A record is a short way to write a class that only holds data.

**`Detector` (interface)**
```java
public interface Detector {
    DataType type();
    List<Detection> detect(String text);
}
```
A promise: "any class that wants to be a detector must provide these two methods". **Why?** The engine can then treat all detectors the same way (polymorphism). Adding a new detector later does not require changing the engine.

### Step 2: Detectors using regular expressions (regex)

A **regex** describes a text *pattern*. Java's `Pattern` compiles it and `Matcher` finds matches. `m.start()`, `m.end()` and `m.group()` give the position and the matched text.

**Phone** (`PhoneDetector`)
```
(?<![A-Za-z0-9])(?:\+91[\s-]?|0)?[6-9]\d{4}[\s-]?\d{5}(?![A-Za-z0-9])
```
| Part | Meaning |
|---|---|
| `(?<![A-Za-z0-9])` | the character before must NOT be a letter or digit |
| `(?:\+91[\s-]?\|0)?` | optional `+91` (with optional space or dash) or a leading `0` |
| `[6-9]\d{4}` | Indian mobiles start with 6, 7, 8 or 9, then 4 more digits |
| `[\s-]?` | optional space or dash in the middle |
| `\d{5}` | last 5 digits |
| `(?![A-Za-z0-9])` | the character after must NOT be a letter or digit |

**Why the "before/after" checks (lookbehind/lookahead)?** Without them, a 16-digit card-like number would also "contain" a phone number. With them, only a complete, standalone number matches.

**Email** (`EmailDetector`)
```
\b[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}\b
```
Name part, then `@`, then domain parts, then a final dot and at least 2 letters (`.com`, `.in`).

**PAN** (`PanDetector`)
```
\b[A-Z]{3}[ABCFGHJLPT][A-Z]\d{4}[A-Z]\b
```
A PAN looks like `ABCPD1234E`: 3 letters, 1 letter that tells the holder type, 1 letter, 4 digits, 1 letter. The 4th letter is only ever one of `A B C F G H J L P T` (P = person, C = company, and so on). Restricting it removes many false matches.

**Aadhaar** (`AadhaarDetector`)
```
(?<!\d)[2-9]\d{3}([ -]?)\d{4}\1\d{4}(?!\d)
```
- First digit is 2-9 (Aadhaar never starts with 0 or 1).
- Written as 4-4-4 digits, with a space, a dash or nothing between groups.
- `\1` is a **back-reference**: it means "use the same separator as before". So `1234 5678 9012` and `1234-5678-9012` match, but `1234 5678-9012` does not.
- A regex only checks the *shape*. A random 12-digit number has the same shape. So we also run the **Verhoeff checksum**.
  **Verhoeff checksum** (`Verhoeff.java`)
  The last digit of a real Aadhaar is a check digit. The algorithm reads digits from right to left, and for each digit it does `c = D[c][P[i % 8][digit]]` using two fixed lookup tables (`D` and `P`). If the final `c` is 0, the number is valid. **Why?** It removes random 12-digit numbers (order IDs, bank references) that only look like Aadhaar.

### Step 3: The engine (`DetectionEngine`)

**What:** Runs every detector on the text and combines the results.

**Why it is needed:** Two detectors can claim overlapping parts of the same text. If both are replaced, the text becomes garbage. So the engine:

1. Collects all findings from all detectors.
2. Sorts them: **longest first**, and for equal length by `DataType` order.
3. Walks through the list and keeps a finding only if it does **not overlap** one already kept.
4. Sorts the survivors by position.
   Two ranges overlap when `a.start < b.end && b.start < a.end`.

`DetectionEngine.withDefaults()` builds an engine with the four built-in detectors.

### Step 4: Masking (`Masker`, `MaskResult`)

**What:** Replace each finding with a token such as `[PHONE_1]`.

**How it works:**
1. Sort findings by position.
2. Keep a `cursor` (how much of the original text is already copied).
3. For each finding: copy the text from `cursor` to the finding's start, write the token, and move `cursor` to the finding's end.
4. After the last finding, copy the rest of the text.
   **Why a cursor instead of `String.replace`?** `replace` swaps *every* occurrence and shifts later positions. The cursor method uses the original positions and cannot go wrong.

**Same value, same token.** The `Masker` remembers `"PHONE:9876543210" -> "[PHONE_1]"` in a `HashMap` (`valueToToken`). If the same number appears three times, all three become `[PHONE_1]`. A per-type counter (`EnumMap<DataType, Integer>`) creates `[PHONE_2]` for a different number. **Why?** The AI can see that it is the same thing, and unmasking is exact.

`MaskResult` is a record holding two things: the `maskedText` to send to the AI and the `tokenToValue` mapping to store in the vault.

### Step 5: Unmasking (`Unmasker`)

**What:** Turn tokens in the AI's reply back into real values.

**The problem:** AI models do not always copy tokens exactly. You may get `[PHONE_1]`, `PHONE_1`, `[phone 1]` or `phone-1`.

**The solution:** A forgiving regex finds anything that looks like `word + number`:
```
(?<![A-Za-z0-9])(\[\s*)?([A-Za-z]+)[ _\-]?(\d+)(\s*\])?
```
Each match is *normalized* to `[WORD_NUMBER]` in upper case, then looked up in the mapping.
- Found: replace with the real value.
- Not found: leave the text untouched (so normal text like "Room 5" stays safe).
  `Matcher.quoteReplacement` is used so that characters like `$` or `\` inside a real value are not misread by the regex engine.

### Step 6: Encryption (`CryptoService`)

**Why encrypt?** The mapping contains your real data. If it were stored as plain text, the whole shield would be pointless.

| Piece | What it does |
|---|---|
| **Password** | Chosen by the user |
| **Salt** (16 random bytes) | Makes the key different even if two people use the same password |
| **PBKDF2-HMAC-SHA256, 600,000 rounds** | Turns the password + salt into a 256-bit key. The many rounds make password guessing very slow |
| **AES-256-GCM** | The cipher. It both hides the data and detects tampering (the *tag*) |
| **IV / nonce** (12 random bytes) | A fresh random value for every encryption. Reusing it with GCM would be a serious security mistake |

Output format: `IV (12 bytes) + ciphertext + tag`. On decrypt, the IV is read from the front. If anyone changed even one byte, decryption fails with an exception instead of returning wrong data.

### Step 7: The Vault (`Vault`, `RetentionPolicy`)

**What:** Stores the encrypted mapping per session ID and deletes it when it should expire.

**`RetentionPolicy`** has three modes:
- `SESSION`: kept only for the current run
- `TTL`: kept for a given time (for example 24 hours), then deleted
- `PERMANENT`: kept until deleted manually
  **How `put` works:** the mapping is converted to bytes, encrypted, and stored together with its policy and expiry time.

**Why Base64 in `serialize`?** Each line is `base64(token):base64(value)`. Real values may contain `:` or new lines, which would break a simple format. Base64 output never contains those characters.

**Expiry:** `get` checks the expiry and deletes the entry if it is too old (*lazy deletion*). `purgeExpired()` removes all expired entries at once.

**Why is `Clock` passed in?** Tests can pass a fake clock and "move time forward" instead of waiting 24 real hours.

**Why `ConcurrentHashMap`?** It is safe when several threads (GUI and background cleanup) use it at the same time.
 
---

## 7. Testing

Run all tests:
```bash
mvn test
```

| Test class | Tests | What it checks |
|---|---|---|
| `PhoneDetectorTest` | 5 | plain 10 digits, `+91`, two numbers, ignores numbers starting with 5, ignores digits inside a longer number |
| `PanDetectorTest` | 5 | valid PAN, two PANs, invalid 4th letter, lower case, PAN inside a longer word |
| `AadhaarDetectorTest` | 6 | valid plain/space/hyphen Aadhaar, wrong checksum, starts with 1, random 12 digits |
| `DetectionEngineTest` | 4 | all types in position order, longer match wins, equal length uses enum order, clean text |
| `MaskerTest` | 6 | single phone, same value same token, different values count up, counters per type, tokens consistent across calls, text without PII unchanged |
| `UnmaskerTest` | 7 | exact tokens, lower case / space / hyphen, no brackets, several tokens, unknown token left alone, ordinary words untouched, round trip with Masker |
| `CryptoServiceTest` | 4 | decrypt returns original, same input encrypts differently, tampered data fails, wrong password fails |
| `VaultTest` | 8 | store and load, delete, unknown session, TTL expiry, permanent entries, purge only expired, stored data is not plain text, full flow mask, vault, unmask |

**Why tests?** A test is a tiny program that checks one behaviour, for example "this phone number must be found". When you change code later, the tests tell you instantly if something broke.

Useful JUnit tools: `@Test`, `assertEquals`, `assertTrue`, `assertFalse`, `assertThrows`, `assertArrayEquals`.
 
---

## 8. Build it yourself from scratch (learning path)

Do these in order. Do not copy and paste. Type the code, run the tests, and read the errors.

1. Create a Maven project (Java 17). Add JUnit 5. Run an empty test.
2. Write `DataType`. Print `DataType.PHONE`. Try a typo and read the compiler error.
3. Write `Detection` and `Detector`.
4. Write `PhoneDetector` using `Pattern` and `Matcher`. Test it with a main method first, then write real tests (valid, invalid, `+91`, inside a longer number).
5. Write `EmailDetector` and `PanDetector` the same way.
6. Write `Verhoeff` and `AadhaarDetector`. Test with a known valid number and one with the last digit changed.
7. Write `DetectionEngine`. Write a test where two detectors overlap.
8. Write `Masker`. First make it work for one detection, then many, then repeated values.
9. Write `Unmasker`. Start with exact tokens, then add the forgiving regex.
10. Write `CryptoService`. Test that decrypting with a wrong password fails.
11. Write `RetentionPolicy` and `Vault`. Use a fake `Clock` to test expiry.
12. Connect everything in `Main`.
    After each step, ask yourself three questions: *What does this do? Why is it needed? What would break without it?*

---

## 9. Known limitations

Be honest about these (and fix them in the roadmap):

- The vault is **in memory only**. When the program stops, the mapping is gone.
- The demo uses a hard-coded password (`demo-password`) and a fresh salt every run. A real app must ask the user for a password and save the salt.
- There is no detector for names or custom words yet.
- Regex detection can have false positives and false negatives. It finds *patterns*, not meaning.
- Only Indian formats (+91 mobiles, PAN, Aadhaar) are supported.
- The unmasker accepts forgiving token formats, but if an AI rewrites a token in a completely different way, it will not be restored.
---

## 10. Roadmap

1. **Custom detector** for user-defined words (names, company, address keywords).
2. **SQLite storage** for the vault, saving the salt, and a background task that calls `purgeExpired()`.
3. **Swing GUI**: text box, Mask button, masked output, Unmask area.
4. **Photo tools**: show and strip EXIF metadata (GPS location, device).
5. **OCR / QR redaction, clipboard shield, dashboard.**
---

## 11. Glossary

| Word | Simple meaning |
|---|---|
| **PII** | Personally Identifiable Information (phone, email, Aadhaar, PAN...) |
| **Token** | A harmless placeholder such as `[PHONE_1]` |
| **Mapping** | A table from token to real value |
| **Regex** | A pattern language for finding text |
| **Lookbehind / lookahead** | A regex check of what comes before or after, without including it in the match |
| **Checksum** | A calculated digit used to check that a number is valid |
| **Enum** | A type with a fixed list of allowed values |
| **Record** | A short way to write a class that only holds data |
| **Interface** | A list of methods that a class promises to provide |
| **Salt** | Random data mixed with a password so keys are unique |
| **IV** | A random per-message value used by the cipher |
| **AES-GCM** | A cipher that encrypts and also detects tampering |
| **PBKDF2** | A slow function that turns a password into a key |
| **TTL** | Time To Live: how long data is kept before deletion |
 
---

## 12. Team

- **Rishav** (Rishav0611): repository owner and team leader
- **Saumya Sachin**: teammate;
